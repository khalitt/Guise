package com.houvven.guise.hook

import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.configs
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import com.houvven.guise.hook.hooker.AccountHooker
import com.houvven.guise.hook.hooker.AdvertisingIdHooker
import com.houvven.guise.hook.hooker.BluetoothHooker
import com.houvven.guise.hook.hooker.ClipboardHooker
import com.houvven.guise.hook.hooker.GpuFingerprintHooker
import com.houvven.guise.hook.hooker.InstallSourceHooker
import com.houvven.guise.hook.hooker.MediaDrmHooker
import com.houvven.guise.hook.hooker.MediaProjectionHooker
import com.houvven.guise.hook.hooker.NetworkHooker
import com.houvven.guise.hook.hooker.PackageHooker
import com.houvven.guise.hook.hooker.ProcFsHooker
import com.houvven.guise.hook.hooker.PropertiesHooker
import com.houvven.guise.hook.hooker.ResourceConfigurationHooker
import com.houvven.guise.hook.hooker.ScreenshotHooker
import com.houvven.guise.hook.hooker.SensorHooker
import com.houvven.guise.hook.hooker.SettingsSecureHooker
import com.houvven.guise.hook.hooker.SystemTimeHooker
import com.houvven.guise.hook.hooker.TelephonyInfoHooker
import com.houvven.guise.hook.hooker.TimezoneHooker
import com.houvven.guise.hook.hooker.UsbHooker
import com.houvven.guise.hook.hooker.UserHooker
import com.houvven.guise.hook.hooker.WallpaperHooker
import com.houvven.guise.hook.hooker.WifiHooker
import com.houvven.guise.hook.hooker.location.CellHooker
import com.houvven.guise.hook.hooker.location.LocationHooker
import com.houvven.guise.hook.hooker.system.location.SysLocationHooker
import com.houvven.guise.hook.store.impl.SharedPreferenceModuleStore

@InjectYukiHookWithXposed(
    modulePackageName = "com.houvven.guise",
    isUsingXposedModuleStatus = true
)
object HookEntry : IYukiHookXposedInit {

    override fun onInit() = configs {
        isDebug = false
        debugLog {
            tag = "GuiseHook"
        }
    }

    override fun onHook() = encase {
        loadAppHooker()
        loadFrameworkHooker()
    }

    private fun PackageParam.loadAppHooker() {
        val store = SharedPreferenceModuleStore.Hooked(packageParam = this)
        val profiles = store.get(mainProcessName)
        val blackList = listOf("android", "com.android.phone", "com.houvven.guise")
        if (packageName in blackList) {
            return
        }
        if (!profiles.isAvailable) {
            YLog.info("No profiles for $packageName")
            return
        }
        loadApp(
            isExcludeSelf = true,
            *buildList {
                // ---------------- 原有 Hooker ----------------
                add(PackageHooker(profiles))
                add(ResourceConfigurationHooker(profiles))
                add(LocationHooker(profiles))
                add(CellHooker(profiles))
                add(SettingsSecureHooker(profiles))
                add(TimezoneHooker(profiles))
                add(NetworkHooker(profiles))
                add(WifiHooker(profiles))
                add(PropertiesHooker(profiles.properties))
                // ---------------- 新增 Hooker (Android 14 / 15 / 16 +) ----------------
                // 账号/谷歌账号伪装
                addIfNeed(
                    !profiles.disableAccounts
                            || profiles.googleAccount != null
                            || profiles.accountName != null
                ) { AccountHooker(profiles) }
                // 电话标识符：IMEI/IMEI2/IMSI/手机号/ICCID/国家代码/运营商
                addIfNeed(
                    profiles.imei != null || profiles.imei2 != null
                            || profiles.imsi != null || profiles.line1Number != null
                            || profiles.simSerialNumber != null || profiles.meid != null
                            || profiles.simCountryIso != null || profiles.networkCountryIso != null
                            || profiles.networkOperator != null
                            || profiles.simOperatorName != null || profiles.networkOperatorName != null
                            || profiles.deviceIdType != null
                ) { TelephonyInfoHooker(profiles) }
                // 蓝牙
                addIfNeed(
                    profiles.disableBluetooth
                            || profiles.bluetoothName != null
                            || profiles.bluetoothAddress != null
                ) { BluetoothHooker(profiles) }
                // 广告ID：GAID / OAID / VAID / AAID / LAT
                addIfNeed(
                    profiles.advertisingId != null || profiles.latEnabled != null
                            || profiles.oaid != null || profiles.vaid != null || profiles.aaid != null
                ) { AdvertisingIdHooker(profiles) }
                // 传感器
                addIfNeed(
                    profiles.disableAllSensors || profiles.disableAccelerometer
                            || profiles.disableGyroscope || profiles.disableMagnetometer
                            || profiles.disableStepCounter
                ) { SensorHooker(profiles) }
                // 用户/工作资料
                addIfNeed(
                    profile = profiles.userId != null || profiles.userSerialNumber != null
                ) { UserHooker(profiles) }
                // USB / 调试
                addIfNeed(
                    profile = profiles.usbStateChargingOnly || profiles.disableUsbDebugging
                ) { UsbHooker(profiles) }
                // 壁纸尺寸
                addIfNeed(
                    profile = profiles.wallpaperDesiredMinimumWidth != null
                            || profiles.wallpaperDesiredMinimumHeight != null
                ) { WallpaperHooker(profiles) }
                // DRM 设备ID (Widevine)
                addIfNeed(profile = profiles.drmDeviceId != null) { MediaDrmHooker(profiles) }
                // 强制截屏：绕过FLAG_SECURE
                addIfNeed(profile = profiles.forceScreenshotEnabled) { ScreenshotHooker(profiles) }
                // ======== 进阶反作弊增强 ========
                // 屏幕录制/共享检测
                addIfNeed(
                    profile = profiles.disableMediaProjection || profiles.fakeVirtualDisplayCount != null
                ) { MediaProjectionHooker(profiles) }
                // 剪贴板
                addIfNeed(
                    profile = profiles.disableClipboardRead || profiles.fakeClipboardText != null
                ) { ClipboardHooker(profiles) }
                // /proc文件系统 Native 指纹
                addIfNeed(
                    profile = profiles.disableProcFingerprint
                            || profiles.fakeCpuModelName != null
                            || profiles.fakeMemTotalKb != null
                ) { ProcFsHooker(profiles) }
                // GPU/GL 渲染器指纹 (glGetString / eglQueryString)
                addIfNeed(
                    profile = profiles.disableGpuFingerprint
                            || profiles.glRenderer != null || profiles.glVendor != null
                            || profiles.glVersion != null || profiles.eglVendor != null
                            || profiles.eglVersion != null
                ) { GpuFingerprintHooker(profiles) }
                // 安装来源 (伪装 Google Play 等)
                addIfNeed(
                    profile = profiles.fakeInstalledFromGooglePlay
                            || profiles.installerPackage != null
                            || profiles.fakeInstallSourceInitiatingPackage != null
                            || profiles.fakeInstallSourceOriginatingPackage != null
                ) { InstallSourceHooker(profiles) }
                // 系统运行时长 / 开机时间
                addIfNeed(
                    profile = profiles.deviceUptimeOffsetMs != null
                            || profiles.fakeBootCompletedTimestampMs != null
                            || profiles.fakeUptimeOneWeekOld
                ) { SystemTimeHooker(profiles) }
            }.toTypedArray()
        )
    }

    /**
     * 便捷工厂：只有当条件为 true 时才构造 hooker，避免加载不必要的 hooker 引起额外性能开销。
     */
    private inline fun <T> MutableList<T>.addIfNeed(profile: Boolean, factory: () -> T) {
        if (profile) add(factory())
    }

    private fun PackageParam.loadFrameworkHooker() {
        loadSystem {
            loadHooker(SysLocationHooker())
        }
    }

}
