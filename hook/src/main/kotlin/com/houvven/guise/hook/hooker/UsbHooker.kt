package com.houvven.guise.hook.hooker

import com.highcapable.betterandroid.system.extension.tool.SystemVersion
import android.provider.Settings
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.factory.toClass
import com.houvven.guise.hook.profile.HookProfiles

/**
 * USB / 调试状态 伪装 Hooker
 * 覆盖：
 *   - UsbManager.getDeviceList()  /  getAccessoryList() ：有调试设备时反作弊系统会判定为模拟器或破解机
 *   - Settings.Global / Settings.Secure 中 ADB_ENABLED / DEVELOPMENT_SETTINGS_ENABLED
 *   - USB 广播的状态（通过BroadcastReceiver过于复杂，这里只拦截系统API读取）
 *   - Build.IS_DEBUGGABLE 一般是系统级，已通过customProperties覆盖；这里主要负责ADB读取
 */
internal class UsbHooker(private val profile: HookProfiles) : YukiBaseHooker() {

    override fun onHook() {
        hookUsbDevices()
        hookAdbEnabled()
        hookUsbConfigured()
    }

    private fun hookUsbDevices() {
        if (profile.disableUsbDebugging || profile.usbStateChargingOnly) {
            val usbManagerClass = runCatching { "android.hardware.usb.UsbManager".toClass() }.getOrNull()
                ?: return
            // 返回空Map表示没有USB设备
            usbManagerClass.method {
                name("getDeviceList")
            }.ignored().hookAll().replaceTo(emptyMap<String, Any>())
            // 返回null表示无 accessory
            usbManagerClass.method {
                name("getAccessoryList")
            }.ignored().hookAll().replaceTo(null)
        }
    }

    private fun hookAdbEnabled() {
        if (profile.disableUsbDebugging) {
            // Settings.Global.getInt(ADB_ENABLED, 0)  Hook ContentResolver query 由 Settings 类触发
            val settingsGlobalClass = "android.provider.Settings\$Global".toClass()
            runCatching {
                settingsGlobalClass.method {
                    name("getInt")
                }.hookAll().before {
                    val key = args.getOrNull(args.size - 1) as? String
                    if (key == "adb_enabled" || key == "development_settings_enabled") {
                        result = 0
                    }
                }
            }
        }
    }

    /**
     * Hook BatteryManager 获取 USB/充电状态
     * 通过 ACTION_BATTERY_CHANGED sticky广播读取时，一般应用走BatteryManager接口
     * 这里Hook BatteryManager.getIntProperty(BATTERY_PLUGGED) 的值
     */
    private fun hookUsbConfigured() {
        if (profile.usbStateChargingOnly) {
            val batteryManagerClass = runCatching { "android.os.BatteryManager".toClass() }.getOrNull()
                ?: return
            runCatching {
                batteryManagerClass.method {
                    name("getIntProperty")
                    param(Int::class.javaPrimitiveType as Class<*>)
                }.hookAll().before {
                    val id = args.first() as Int
                    // BatteryManager.BATTERY_PLUGGED_ANY 的plugged类型为USB/AC/Wireless均归为充电
                    // BATTERY_STATUS_CHARGING 为2
                    val pluggedField = runCatching {
                        batteryManagerClass.getField("BATTERY_PROPERTY_PLUGGED").getInt(null)
                    }.getOrDefault(6)
                    if (id == pluggedField) {
                        // BatteryManager.BATTERY_PLUGGED_AC = 1  （纯交流充电，非USB）
                        result = 1
                    }
                }
            }
        }
    }
}
