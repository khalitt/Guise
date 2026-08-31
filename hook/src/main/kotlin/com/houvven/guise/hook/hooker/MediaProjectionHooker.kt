package com.houvven.guise.hook.hooker

import android.os.Build
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.factory.toClass
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.houvven.guise.hook.profile.HookProfiles

/**
 * 屏幕录制/共享 伪装 Hooker
 *
 * 反作弊检测：
 *   - MediaProjectionManager.getActiveProjectionTokenCount()  (Android 14+)
 *   - DisplayManager.getDisplays() 里是否存在 VIRTUAL / DISPLAY_FLAG_PRIVATE 显示
 *   - WindowManager 回调的 TYPE_VIRTUAL_DISPLAY 覆盖层  (录屏/投屏常见)
 *   - MediaProjectionCallback.onCapturedContentVisibilityChanged / onStop
 *
 * 处理：
 *   - disableMediaProjection=true → 所有"当前是否在录制/投影"查询返回 0 / null / false
 *   - fakeVirtualDisplayCount=N 可自定义虚拟显示数量
 */
internal class MediaProjectionHooker(private val profile: HookProfiles) : YukiBaseHooker() {

    override fun onHook() {
        if (!profile.disableMediaProjection && profile.fakeVirtualDisplayCount == null) return

        hookActiveProjectionToken()
        hookVirtualDisplays()
        hookDisplayFlags()
    }

    /** Android 14 新增: MediaProjectionManager.getActiveProjectionTokenCount() */
    private fun hookActiveProjectionToken() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        val mpmClass = runCatching {
            "android.media.projection.MediaProjectionManager".toClass()
        }.getOrNull() ?: return

        val fakeCount = if (profile.disableMediaProjection) 0 else profile.fakeVirtualDisplayCount
        runCatching {
            mpmClass.method {
                name("getActiveProjectionTokenCount")
            }.hookAll().apply {
                fakeCount?.let { replaceTo(it) }
            }
        }

        // 老版本接口：getMediaProjection(...) 返回 null 来让后续无法启动
        if (profile.disableMediaProjection) {
            runCatching {
                mpmClass.method {
                    name("getMediaProjection")
                    param(IntType, Any::class.java)
                }.hookAll().replaceTo(null)
            }
        }
    }

    /**
     * DisplayManager.getDisplays() 过滤掉 FLAG_VIRTUAL 类型的 Display
     * 录屏APP会创建 VirtualDisplay，反作弊会枚举所有Display检测之
     */
    private fun hookVirtualDisplays() {
        if (!profile.disableMediaProjection) return
        val dmClass = runCatching { "android.hardware.display.DisplayManager".toClass() }.getOrNull()
            ?: return
        val displayClass = "android.view.Display".toClass()
        val displayFlagVirtualField = runCatching {
            displayClass.getField("FLAG_VIRTUAL").apply { isAccessible = true }.getInt(null)
        }.getOrElse { 1 shl 4 } // 0x10 兼容

        val getDisplaysMethods = listOf(
            runCatching { dmClass.method { name("getDisplays") } }.getOrNull(),
            runCatching {
                dmClass.method {
                    name("getDisplays")
                    param(IntType)
                }
            }.getOrNull()
        )
        getDisplaysMethods.forEach { method ->
            method?.hookAll()?.after {
                val arr = result as? Array<*> ?: return@after
                val filtered = arr.filter { d ->
                    if (d == null) false
                    else {
                        val flags = runCatching {
                            displayClass.getMethod("getFlags").invoke(d) as Int
                        }.getOrDefault(0)
                        flags and displayFlagVirtualField == 0  // 保留非 VIRTUAL
                    }
                }
                val target = java.lang.reflect.Array.newInstance(displayClass, filtered.size)
                filtered.forEachIndexed { index, display ->
                    java.lang.reflect.Array.set(target, index, display)
                }
                result = target
            }
        }
    }

    /** 兜底：WindowManager.getDefaultDisplay 返回的 display 也去掉 FLAG_PRIVATE */
    private fun hookDisplayFlags() {
        if (!profile.disableMediaProjection) return
        val displayClass = "android.view.Display".toClass()
        runCatching {
            displayClass.method { name("getFlags") }.hookAll().after {
                val flags = result as? Int ?: return@after
                val private = runCatching {
                    displayClass.getField("FLAG_PRIVATE").getInt(null)
                }.getOrElse { 1 shl 5 }
                result = flags and private.inv()
            }
        }
    }
}
