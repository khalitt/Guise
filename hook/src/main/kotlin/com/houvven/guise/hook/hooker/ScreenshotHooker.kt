package com.houvven.guise.hook.hooker

import android.app.Activity
import android.os.Build
import android.view.Window
import android.view.WindowManager
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.factory.toClass
import com.highcapable.yukihookapi.hook.type.java.BooleanType
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.houvven.guise.hook.profile.HookProfiles

/**
 * 强制截屏 Hooker
 *
 * 目标：移除/绕过 APP 设置的"禁止截屏/录屏"保护。
 *   1) WindowManager.LayoutParams.FLAG_SECURE  (经典防截屏标志，全版本通用)
 *   2) Android 14 新增  Window.setPrivacyMode(true) / LayoutParams.privateFlags
 *   3) Activity.setRecentsScreenshotEnabled(false)  (最近任务缩略图)
 *   4) Surface / SurfaceControl.setSecure(true)    (Android 12+ 显示链路)
 *
 * Hook 入口：
 *   - Window.setFlags / addFlags / clearFlags / setAttributes → 统一移除 FLAG_SECURE
 *   - Window.getAttributes() 返回前再清理一次，做双保险
 *   - Activity.onResume / onPostResume → 生命周期二次清理 + 开最近任务缩略图
 *   - LayoutParams 构造器 / setPrivacyMode → Android 14 兜底
 *   - SurfaceControl.setSecure / Surface.setSecure → 最终安全网
 */
internal class ScreenshotHooker(private val profile: HookProfiles) : YukiBaseHooker() {

    override fun onHook() {
        if (!profile.forceScreenshotEnabled) return

        hookWindowSetFlags()
        hookWindowAddFlags()
        hookWindowSetAttributes()
        hookWindowGetAttributes()
        hookActivityLifecycle()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            hookPrivacyMode()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hookSurfaceControl()
        }
    }

    // --------------------------------------------------------
    // Window.setFlags(flags, mask)  — 主入口
    // --------------------------------------------------------
    private fun hookWindowSetFlags() {
        Window::class.java.method {
            name("setFlags")
            param(IntType, IntType)
        }.hook().before {
            val flags = args[0] as Int
            // 把 FLAG_SECURE 从写入值和 mask 中同时移除，避免后续再次被 apply 回来
            args[0] = flags and FLAG_SECURE.inv()
            val mask = args[1] as Int
            args[1] = mask and FLAG_SECURE.inv()
        }
    }

    // --------------------------------------------------------
    // Window.addFlags(flags) — 直接忽略掉 FLAG_SECURE
    // --------------------------------------------------------
    private fun hookWindowAddFlags() {
        Window::class.java.method {
            name("addFlags")
            param(IntType)
        }.hook().before {
            val flags = args[0] as Int
            args[0] = flags and FLAG_SECURE.inv()
        }
    }

    // --------------------------------------------------------
    // Window.setAttributes(LayoutParams)  — 直接传LayoutParams的路径
    // Flutter / RN / Dialog 等大量场景直接走这里
    // --------------------------------------------------------
    private fun hookWindowSetAttributes() {
        Window::class.java.method {
            name("setAttributes")
            param(WindowManager.LayoutParams::class.java)
        }.hook().before {
            val attrs = args[0] as? WindowManager.LayoutParams ?: return@before
            attrs.flags = attrs.flags and FLAG_SECURE.inv()
            args[0] = attrs
        }
    }

    // --------------------------------------------------------
    // 兜底：Window.getAttributes() 返回前清理
    // --------------------------------------------------------
    private fun hookWindowGetAttributes() {
        Window::class.java.method {
            name("getAttributes")
        }.hookAll().after {
            val attrs = result as? WindowManager.LayoutParams ?: return@after
            attrs.flags = attrs.flags and FLAG_SECURE.inv()
            result = attrs
        }
    }

    // --------------------------------------------------------
    // Activity 生命周期：二次清理 + 最近任务截图允许
    // --------------------------------------------------------
    private fun hookActivityLifecycle() {
        listOf("onResume", "onStart", "onPostResume").forEach { methodName ->
            Activity::class.java.method {
                name(methodName)
            }.ignored().hookAll().after {
                val activity = instance as? Activity ?: return@after
                activity.window?.clearFlags(FLAG_SECURE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    runCatching { activity.setRecentsScreenshotEnabled(true) }
                }
            }
        }
    }

    // --------------------------------------------------------
    // Android 14 (API 34) PrivacyMode
    // --------------------------------------------------------
    private fun hookPrivacyMode() {
        runCatching {
            WindowManager.LayoutParams::class.java.method {
                name("setPrivacyMode")
                param(BooleanType)
            }.hookAll().before {
                args[0] = false
            }
        }
        // Window 级 setPrivacyMode
        runCatching {
            Window::class.java.method {
                name("setPrivacyMode")
                param(BooleanType)
            }.hookAll().before {
                args[0] = false
            }
        }
    }

    // --------------------------------------------------------
    // SurfaceControl / Surface 最终兜底
    // --------------------------------------------------------
    private fun hookSurfaceControl() {
        runCatching {
            "android.view.SurfaceControl".toClass().method {
                name("setSecure")
                param(BooleanType)
            }.hookAll().before {
                args[0] = false
            }
        }
        runCatching {
            android.view.Surface::class.java.method {
                name("setSecure")
                param(BooleanType)
            }.hookAll().before {
                args[0] = false
            }
        }
    }

    companion object {
        private const val FLAG_SECURE = WindowManager.LayoutParams.FLAG_SECURE
    }
}
