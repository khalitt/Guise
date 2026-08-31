package com.houvven.guise.hook.hooker

import android.os.SystemClock
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.factory.toClass
import com.houvven.guise.hook.profile.HookProfiles

/**
 * 系统运行时长 / 开机时间 伪装 Hooker
 *
 * 反作弊：
 *   - 新刷机设备刚开机（uptime < 1小时）会被标为高风险
 *   - elapsedRealtime() 与实际设备创建时间对照可以判断"是否刚换环境"
 *   - currentThreadTimeMillis / System.nanoTime 差值也可判断
 *
 * 支持：
 *   - fakeUptimeOneWeekOld=true → 加 7 * 24 * 3600 * 1000 ms = 604,800,000 ms
 *   - deviceUptimeOffsetMs=xxx  →  自定义偏移（毫秒，仅建议正值）
 *   - fakeBootCompletedTimestampMs → 对 ACTION_BOOT_COMPLETED 等广播内读取也生效（通过System.currentTimeMillis + offset）
 *
 * Hook 目标：
 *   - android.os.SystemClock.uptimeMillis()  / elapsedRealtime()
 *   - android.os.SystemClock.currentThreadTimeMillis() / currentThreadTimeMicro()
 *   - java.lang.System.nanoTime()  (同步缩放)
 *   - /proc/stat 中的开机时间（通过 ProcFsHooker 一并处理）
 */
internal class SystemTimeHooker(private val profile: HookProfiles) : YukiBaseHooker() {

    private val offsetMs: Long by lazy {
        val ms = profile.deviceUptimeOffsetMs ?: 0L
        val weekMs = if (profile.fakeUptimeOneWeekOld) ONE_WEEK_MS else 0L
        ms.coerceAtLeast(0L) + weekMs
    }
    private val offsetNs: Long by lazy { offsetMs * 1_000_000L }

    private val enabled: Boolean by lazy { offsetMs > 0L }

    override fun onHook() {
        if (!enabled) return
        hookSystemClock()
        hookJavaLangSystem()
        hookDebugThreadTime()
    }

    private fun hookSystemClock() {
        val scClass = SystemClock::class.java

        // uptimeMillis()
        scClass.method { name("uptimeMillis") }.hookAll().after {
            val v = (result as? Long) ?: return@after
            result = v + offsetMs
        }
        // elapsedRealtime() — 含深度睡眠时间
        scClass.method { name("elapsedRealtime") }.hookAll().after {
            val v = (result as? Long) ?: return@after
            result = v + offsetMs
        }
        scClass.method { name("elapsedRealtimeNanos") }.hookAll().after {
            val v = (result as? Long) ?: return@after
            result = v + offsetNs
        }
        scClass.method { name("uptimeNanos") }.ignored().hookAll().after {
            val v = (result as? Long) ?: return@after
            result = v + offsetNs
        }
        // Thread time
        scClass.method { name("currentThreadTimeMillis") }.hookAll().after {
            val v = (result as? Long) ?: return@after
            result = v + offsetMs
        }
        scClass.method { name("currentThreadTimeMicro") }.hookAll().after {
            val v = (result as? Long) ?: return@after
            result = v + offsetMs * 1000L
        }
    }

    /** System.nanoTime() 也一起偏移，避免APP两边对照产生偏差 */
    private fun hookJavaLangSystem() {
        java.lang.System::class.java.method {
            name("nanoTime")
        }.hookAll().after {
            val v = (result as? Long) ?: return@after
            result = v + offsetNs
        }
    }

    /**
     * android.os.Debug.threadCpuTimeNanos()  (可选)
     * 一些系统接口读取线程CPU时间
     */
    private fun hookDebugThreadTime() {
        runCatching {
            "android.os.Debug".toClass().method {
                name("threadCpuTimeNanos")
            }.hookAll().after {
                val v = (result as? Long) ?: return@after
                result = v + offsetNs
            }
        }
        runCatching {
            "android.os.Debug".toClass().method {
                name("getThreadCpuTime")
            }.hookAll().after {
                val v = (result as? Long) ?: return@after
                if (v > 0) result = v + offsetNs
            }
        }
    }

    companion object {
        private const val ONE_WEEK_MS = 7L * 24L * 60L * 60L * 1000L
    }
}
