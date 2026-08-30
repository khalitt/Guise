package com.houvven.guise.hook.hooker

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.factory.toClass
import com.houvven.guise.hook.profile.HookProfiles

/**
 * 壁纸管理器 Hooker
 * 反作弊常见检测：
 *   - WallpaperManager.getDesiredMinimumWidth() / getDesiredMinimumHeight()
 *     部分模拟器返回0或固定值，真实设备根据屏幕DPI返回固定非零值，可能被做指纹特征
 *   - WallpaperManager.getWallpaperId(which) / getWallpaperInfo  无壁纸的模拟器同样有固定值
 *   - Android 14+ 新增 FLAG_ALLOW_OFFLOAD / isWallpaperSupported 接口
 */
internal class WallpaperHooker(private val profile: HookProfiles) : YukiBaseHooker() {

    override fun onHook() {
        val wallpaperManagerClass = runCatching {
            "android.app.WallpaperManager".toClass()
        }.getOrNull() ?: return

        profile.wallpaperDesiredMinimumWidth?.let { w ->
            wallpaperManagerClass.method {
                name("getDesiredMinimumWidth")
            }.ignored().hookAll().replaceTo(w)
        }
        profile.wallpaperDesiredMinimumHeight?.let { h ->
            wallpaperManagerClass.method {
                name("getDesiredMinimumHeight")
            }.ignored().hookAll().replaceTo(h)
        }
    }
}
