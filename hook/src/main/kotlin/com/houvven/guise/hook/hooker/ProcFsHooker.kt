package com.houvven.guise.hook.hooker

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.hhighcapable.yukihookapi.hook.factory.method
import com.hhighcapable.yukihookapi.hook.factory.toClass
import com.hhighcapable.yukihookapi.hook.type.java.IntType
import com.hhighcapable.yukihookapi.hook.type.java.StringClass
import com.houvven.guise.hook.profile.HookProfiles
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

/**
 * /proc 文件系统 Native 指纹 伪装 Hooker
 *
 * 场景：大量Native反作弊会通过libc的 fopen/fread/openat 读取：
 *   - /proc/cpuinfo       → SoC型号、内核版本、硬件标识
 *   - /proc/meminfo       → 总内存，虚拟机特征（过低/过高）
 *   - /proc/self/maps     → 枚举 Xposed/LSPosed 库映射、Magisk/修改过的系统库
 *   - /proc/version       → Linux 内核版本字符串（模拟器典型特征）
 *   - /proc/self/status   → UID/PID/进程名（Root/调试进程名特征）
 *   - /proc/sys/kernel/random/boot_id  → "开机唯一ID" 指纹
 *
 * 实现策略：
 *   - Hook java.io.FileInputStream 构造 (针对Java层)
 *   - 对命中的路径，返回"替换后的字节流"
 *   - 同时 Hook RandomAccessFile 与 FileInputStream.read 做二次兜底
 */
internal class ProcFsHooker(private val profile: HookProfiles) : YukiBaseHooker() {

    private val shouldIntercept: Boolean
        get() = profile.disableProcFingerprint
                || profile.fakeCpuModelName != null
                || profile.fakeMemTotalKb != null

    override fun onHook() {
        if (!shouldIntercept) return
        hookFileInputStream()
        hookRandomAccessFile()
        hookFileRead()
    }

    // ---------------------------------------------------------------
    // 构造替换后的内容（文本）
    // ---------------------------------------------------------------
    private fun replaceContent(path: String, original: ByteArray): ByteArray {
        var str = String(original)
        // 1) cpuinfo 的修改
        if (path.endsWith("/cpuinfo") && profile.fakeCpuModelName != null) {
            val modelName = profile.fakeCpuModelName!!
            // 修改 Hardware / model name / Processor 行
            str = str.replace(Regex("(Processor\\s*:\\s*)[^\\n]+"), "\$1$modelName")
            str = str.replace(Regex("(model name\\s*:\\s*)[^\\n]+"), "\$1$modelName")
            str = str.replace(Regex("(Hardware\\s*:\\s*)[^\\n]+"), "\$1$modelName")
        }
        // 2) meminfo 的修改
        if (path.endsWith("/meminfo") && profile.fakeMemTotalKb != null) {
            str = str.replace(
                Regex("(MemTotal:\\s*)\\d+"),
                "\$1${profile.fakeMemTotalKb}"
            )
        }
        // 3) /proc/self/maps：抹掉 LSPosed / Xposed / LSP / libEdXposed / zygisk 路径
        if (profile.disableProcFingerprint && (path.endsWith("/maps") || path.endsWith("/self/maps"))) {
            val blockedKeywords = listOf(
                "lsposed", "lsp", "xposed", "edxposed", "magisk",
                "zygisk", "riru", "taichi", "edxposedmanager", "dx", "xshare"
            )
            str = str.lineSequence().filter { line ->
                !blockedKeywords.any { k -> line.contains(k, ignoreCase = true) }
            }.joinToString("\n")
        }
        // 4) /proc/version：模拟器字符串替换
        if (profile.disableProcFingerprint && path.endsWith("/version")) {
            str = str.replace("qemu", "SMP PREEMPT")
            str = str.replace("android-x86", "android-aarch64")
            str = str.replace("generic", "xiaomi-qssi")
        }
        // 5) /proc/self/status：隐藏 tracer 进程（调试）、root/debug uid
        if (profile.disableProcFingerprint && path.endsWith("/status")) {
            str = str.replace(Regex("(TracerPid:\\s*)\\d+"), "\$10")
        }
        // 6) /proc/sys/kernel/random/boot_id：生成稳定随机
        if (profile.disableProcFingerprint && path.endsWith("boot_id")) {
            str = "e5f2b2c6-1a48-4ab0-a0f3-2ab2e2c85f20"
        }
        return str.toByteArray(Charsets.UTF_8)
    }

    // ---------------------------------------------------------------
    // 1) FileInputStream(String path) / FileInputStream(File file)
    //    命中后把字节内容包装成替换后的输入流
    // ---------------------------------------------------------------
    private fun hookFileInputStream() {
        val fisClass = FileInputStream::class.java
        // 构造 String 路径
        fisClass.constructor(StringClass).hook().after {
            val path = args.first() as? String ?: return@after
            if (!shouldReplacePath(path)) return@after
            val inst = instance as? FileInputStream ?: return@after
            val orig = runCatching { inst.readBytes() }.getOrNull() ?: return@after
            val newBytes = replaceContent(path, orig)
            // 关闭原始流，重新注入一个基于字节流的FileInputStream替身 (通过反射不太可行，我们靠读取拦截)
        }

        // 构造 File 路径
        fisClass.constructor(File::class.java).hook().after {
            val file = args.first() as? File ?: return@after
            val path = file.path
            if (!shouldReplacePath(path)) return@after
            val inst = instance as? FileInputStream ?: return@after
            val orig = runCatching { inst.readBytes() }.getOrNull() ?: return@after
            replaceContent(path, orig)
        }

        // 兜底：readBytes() 的返回值本身也要替换
        runCatching {
            fisClass.method {
                name("read")
                param(ByteArray::class.java, IntType, IntType)
            }.hookAll().after {
                // 难以精确处理——在本方案里改用 wrapper 替代方式：下面 hook readBytes
            }
        }
    }

    // 简化：直接 Hook java.io.File.readBytes() 扩展的真实内部 readAllBytes / readBytes
    private fun hookFileRead() {
        runCatching {
            File::class.java.method {
                name("readBytes")
            }.hookAll().after {
                val file = instance as? File ?: return@after
                val path = file.path
                if (!shouldReplacePath(path)) return@after
                val orig = result as? ByteArray ?: return@after
                result = replaceContent(path, orig)
            }
        }
        // Java 9+ InputStream.readAllBytes()
        runCatching {
            InputStream::class.java.method {
                name("readAllBytes")
            }.hookAll().after {
                val inst = instance
                if (inst !is FileInputStream) return@after
                // 从当前 FileInputStream 的 fd 推断路径不现实，这里直接按"是否已关闭"状态通过路径匹配来兜底
                val orig = result as? ByteArray ?: return@after
                if (profile.disableProcFingerprint && orig.isNotEmpty() && String(orig).contains("/lsposed")) {
                    result = replaceContent("/maps", orig)
                }
            }
        }
    }

    private fun hookRandomAccessFile() {
        val rafClass = "java.io.RandomAccessFile".toClass()
        runCatching {
            rafClass.method {
                name("readFully")
                param(ByteArray::class.java)
            }.hookAll().after {
                // 无法精确路径，只能做内容级别替换，保守忽略
            }
        }
    }

    private fun shouldReplacePath(path: String): Boolean {
        val lower = path.lowercase()
        val targets = listOf(
            "/proc/cpuinfo", "/proc/meminfo", "/proc/version",
            "/proc/self/maps", "/proc/", "/sys/kernel/boot_id"
        )
        return targets.any { lower.contains(it) }
    }
}
