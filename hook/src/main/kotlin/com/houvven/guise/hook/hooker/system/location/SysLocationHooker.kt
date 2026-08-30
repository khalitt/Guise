package com.houvven.guise.hook.hooker.system.location

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker

/**
 * System Server 级别的 Location Hooker（可选增强）
 * 针对 framework 层的 com.android.server.location.LocationManagerService
 * 拦截 getLastLocation / reportLocation 等接口，实现全局的定位伪造。
 *
 * 注：当前版本为占位实现。在 LSPosed 环境中若需要伪造系统级别的 FusedLocationProvider
 * 结果，可在此扩展 hook LocationManagerService / GeofenceManager / GnssLocationProvider。
 */
internal class SysLocationHooker : YukiBaseHooker() {

    override fun onHook() {
        // 占位：如需系统框架层定位Hook，可在此针对 LocationManagerServiceClassName
        // （见 FrameworkCompontentTypeFactory）通过反射定位并hook。
    }
}
