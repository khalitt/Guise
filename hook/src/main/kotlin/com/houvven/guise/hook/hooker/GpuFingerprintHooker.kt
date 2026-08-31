package com.houvven.guise.hook.hooker

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.factory.toClass
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.houvven.guise.hook.profile.HookProfiles

/**
 * GPU / OpenGL / EGL 渲染器指纹伪装 Hooker
 *
 * 反作弊会调用 GLES20/30.glGetString(name) 获取：
 *   GL_VENDOR = 0x1F00   → 芯片厂商 (Qualcomm / ARM / Imagination / Google (Swiftshader 模拟器))
 *   GL_RENDERER = 0x1F01 → 渲染器型号 (Adreno xxx / Mali-xxx)
 *   GL_VERSION  = 0x1F02 → 驱动版本号，可识别模拟器特定驱动
 *   GL_EXTENSIONS= 0x1F03 → 扩展字符串
 *   EGL_VENDOR / EGL_VERSION
 * 其中模拟器(Genymotion/AS Emulator/雷电) 有 Google/Swiftshader / Android Emulator 关键字
 *
 * 实现方法：Hook android.opengl.GLES20 / GLES30 / EGL10 / EGL14 的 glGetString / eglQueryString
 */
internal class GpuFingerprintHooker(private val profile: HookProfiles) : YukiBaseHooker() {

    private val intercept: Boolean by lazy {
        profile.disableGpuFingerprint
                || profile.glRenderer != null || profile.glVendor != null
                || profile.glVersion != null || profile.eglVendor != null || profile.eglVersion != null
    }

    override fun onHook() {
        if (!intercept) return
        hookGlGetString("android.opengl.GLES20")
        hookGlGetString("android.opengl.GLES30")
        hookGlGetString("android.opengl.GLES31")
        hookGlGetString("android.opengl.GLES32")
        hookEglQueryString()
    }

    // glGetString(int name)  GL_VENDOR=0x1F00  GL_RENDERER=0x1F01  GL_VERSION=0x1F02  GL_EXTENSIONS=0x1F03
    private fun hookGlGetString(className: String) {
        val glClass = runCatching { className.toClass() }.getOrNull() ?: return
        glClass.method {
            name("glGetString")
            param(IntType)
        }.ignored().hookAll().after {
            val name = args.first() as Int
            result = when (name) {
                GL_VENDOR -> profile.glVendor
                    ?: if (profile.disableGpuFingerprint) FAKED_VENDOR else result as? String
                GL_RENDERER -> profile.glRenderer
                    ?: if (profile.disableGpuFingerprint) FAKED_RENDERER else result as? String
                GL_VERSION -> profile.glVersion
                    ?: if (profile.disableGpuFingerprint) FAKED_GL_VERSION else result as? String
                GL_EXTENSIONS -> if (profile.disableGpuFingerprint) "" else result as? String
                else -> result as? String
            }
        }

        // GLES30+ glGetStringi(uint name, uint index) → 返回空字符串，破坏枚举扩展
        if (profile.disableGpuFingerprint) {
            runCatching {
                glClass.method {
                    name("glGetStringi")
                    param(IntType, IntType)
                }.hookAll().replaceTo("")
            }
        }
    }

    private fun hookEglQueryString() {
        // EGL 提供两套接口：EGL10.eglQueryString(EGLDisplay dpy, int name)
        val egClasses = listOf(
            "android.opengl.EGL14",
            "javax.microedition.khronos.egl.EGL10",
            "com.google.android.gles_jni.EGLImpl"
        )
        val fakeVendor = profile.eglVendor ?: (if (profile.disableGpuFingerprint) "Android" else null)
        val fakeVersion = profile.eglVersion ?: (if (profile.disableGpuFingerprint) "1.4 Android" else null)
        egClasses.forEach { name ->
            val cls = runCatching { name.toClass() }.getOrNull() ?: return@forEach
            runCatching {
                cls.method {
                    name("eglQueryString")
                }.hookAll().after {
                    val last = args.lastOrNull() as? Int ?: return@after
                    result = when (last) {
                        EGL_VENDOR -> fakeVendor ?: result
                        EGL_VERSION -> fakeVersion ?: result
                        EGL_EXTENSIONS -> if (profile.disableGpuFingerprint) "" else result
                        else -> result
                    }
                }
            }
        }
    }

    companion object {
        // GL constants
        private const val GL_VENDOR = 0x1F00
        private const val GL_RENDERER = 0x1F01
        private const val GL_VERSION = 0x1F02
        private const val GL_EXTENSIONS = 0x1F03

        private const val EGL_VENDOR = 0x3053
        private const val EGL_VERSION = 0x3054
        private const val EGL_EXTENSIONS = 0x3055

        // 典型高通骁龙 8 系老设备 指纹 (可有效伪装成真机)
        private const val FAKED_VENDOR = "Qualcomm Inc."
        private const val FAKED_RENDERER = "Adreno (TM) 640"
        private const val FAKED_GL_VERSION = "OpenGL ES 3.2 V@0.000.0 (GIT@xxxx)"
    }
}
