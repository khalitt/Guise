package com.houvven.guise.ui.screen.profile.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.houvven.guise.R
import com.houvven.guise.data.domain.ProfileSuggest
import com.houvven.guise.data.repository.profile.AndroidIdRandomRepo
import com.houvven.guise.data.repository.profile.MobileNetworkTypeRepo
import com.houvven.guise.data.repository.profile.NetworkType
import com.houvven.guise.data.repository.profile.ProfilesPlaceholderRepo
import com.houvven.guise.data.repository.profile.ProfilesSuggestRepo
import com.houvven.guise.data.repository.profile.ProfilesSuggestRepo_Enum
import com.houvven.guise.hook.profile.HookProfiles
import com.houvven.guise.hook.profile.item.PropertiesProfile
import org.koin.java.KoinJavaComponent.inject

typealias Profiles = HookProfiles

sealed class ProfileReviseContract {
    open val span = ProfileReviseColumSpan.DEFAULT
}

enum class ProfileReviseColumSpan {
    FULL,
    DEFAULT
}

class ProfileReviseHeader(
    val title: @Composable () -> String
) : ProfileReviseContract() {
    override val span = ProfileReviseColumSpan.FULL
}

sealed class ProfileReviseEditor : ProfileReviseContract() {

    data object None : ProfileReviseEditor()

    sealed class Editor<T> : ProfileReviseEditor() {
        abstract val label: @Composable () -> String
        abstract val value: Profiles.() -> T?
        abstract val onValueClear: Profiles.() -> Profiles

        open val display: (T?) -> String = { it.toString() }
        open val validator: (T?) -> Boolean = { true }

        val placeholder get() = ProfilesPlaceholderRepo.get(value).run(display)

        fun isEdited(profiles: Profiles) = value.invoke(profiles) != null
    }

    class Text(
        override val label: @Composable () -> String,
        override val value: Profiles.() -> String?,
        val onValueChange: Profiles.(String?) -> Profiles,
        val suggestRepo: ProfilesSuggestRepo? = null,
        override val onValueClear: Profiles.() -> Profiles = { onValueChange(null) }
    ) : Editor<String>()

    class TextNumber<T : Number>(
        override val label: @Composable () -> String,
        override val value: Profiles.() -> T?,
        val onValueChange: Profiles.(T?) -> Profiles,
        val stringToNumber: (String) -> T?,
        val suggestRepo: ProfilesSuggestRepo? = null,
        override val onValueClear: Profiles.() -> Profiles = { onValueChange(null) },
    ) : Editor<T>()

    open class Enum<T>(
        override val label: @Composable () -> String,
        override val value: Profiles.() -> T?,
        override val onValueClear: Profiles.() -> Profiles,
        val options: ProfilesSuggestRepo_Enum.() -> List<ProfileSuggest<T>>,
        open val onSelectedChange: Profiles.(ProfileSuggest<T>) -> Profiles,
    ) : Editor<T>()

    class BooleanEnum(
        override val label: @Composable () -> String,
        override val value: Profiles.() -> Boolean?,
        override val onValueClear: Profiles.() -> Profiles,
        override val onSelectedChange: Profiles.(ProfileSuggest<Boolean>) -> Profiles
    ) : Enum<Boolean>(
        label = label,
        value = value,
        onValueClear = onValueClear,
        onSelectedChange = onSelectedChange,
        options = { boolean }
    )
}


// ================================================================
//  1. Properties - 原有的系统属性 + 新增Hardware/Build.VERSION
// ================================================================
private val PropertiesReviseItems = listOf(
    ProfileReviseHeader { stringResource(id = R.string.system_properties) },
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.brand) },
        value = { properties.brand },
        onValueChange = { properties { copy(brand = it) } }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.manufacturer) },
        value = { properties.manufacturer },
        onValueChange = { properties { copy(manufacturer = it) } },
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.model) },
        value = { properties.model },
        onValueChange = { properties { copy(model = it) } },
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.device) },
        value = { properties.device },
        onValueChange = { properties { copy(device = it) } },
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.product) },
        value = { properties.product },
        onValueChange = { properties { copy(product = it) } }
    ),
    ProfileReviseEditor.Enum(
        label = { stringResource(id = R.string.characteristic) },
        value = { properties.characteristics },
        options = { characteristics },
        onValueClear = { properties { copy(characteristics = null) } },
        onSelectedChange = { properties { copy(characteristics = it.value) } },
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.build_display_id) },
        value = { properties.displayId },
        onValueChange = { properties { copy(displayId = it) } }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.fingerprint) },
        value = { properties.fingerprint },
        onValueChange = { properties { copy(fingerprint = it) } }
    )
)

private val HardwarePropertiesReviseItems = listOf(
    ProfileReviseHeader { stringResource(id = R.string.hardware_properties) },
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.hardware) },
        value = { properties.hardware },
        onValueChange = { properties { copy(hardware = it) } }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.board) },
        value = { properties.board },
        onValueChange = { properties { copy(board = it) } }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.bootloader) },
        value = { properties.bootloader },
        onValueChange = { properties { copy(bootloader = it) } }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.radio_version) },
        value = { properties.radioVersion },
        onValueChange = { properties { copy(radioVersion = it) } }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.serial) },
        value = { properties.serial },
        onValueChange = { properties { copy(serial = it) } }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.soc_model) },
        value = { properties.socModel },
        onValueChange = { properties { copy(socModel = it) } }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.soc_manufacturer) },
        value = { properties.socManufacturer },
        onValueChange = { properties { copy(socManufacturer = it) } }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.odm_sku) },
        value = { properties.odmSku },
        onValueChange = { properties { copy(odmSku = it) } }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.sku) },
        value = { properties.sku },
        onValueChange = { properties { copy(sku = it) } }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.vbmeta_digest) },
        value = { properties.vbmetaDigest },
        onValueChange = { properties { copy(vbmetaDigest = it) } }
    )
)

private val BuildVersionReviseItems = listOf(
    ProfileReviseHeader { stringResource(id = R.string.build_version_properties) },
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.android_release) },
        value = { properties.release },
        onValueChange = { properties { copy(release = it) } }
    ),
    ProfileReviseEditor.TextNumber(
        label = { stringResource(id = R.string.sdk_int) },
        value = { properties.sdkInt },
        onValueChange = { properties { copy(sdkInt = it) } },
        stringToNumber = { it.toIntOrNull() }
    ),
    ProfileReviseEditor.TextNumber(
        label = { stringResource(id = R.string.preview_sdk_int) },
        value = { properties.previewSdkInt },
        onValueChange = { properties { copy(previewSdkInt = it) } },
        stringToNumber = { it.toIntOrNull() }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.build_incremental) },
        value = { properties.incremental },
        onValueChange = { properties { copy(incremental = it) } }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.build_codename) },
        value = { properties.codename },
        onValueChange = { properties { copy(codename = it) } }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.security_patch) },
        value = { properties.securityPatch },
        onValueChange = { properties { copy(securityPatch = it) } }
    ),
    ProfileReviseEditor.TextNumber(
        label = { stringResource(id = R.string.media_performance_class) },
        value = { properties.mediaPerformanceClass },
        onValueChange = { properties { copy(mediaPerformanceClass = it) } },
        stringToNumber = { it.toIntOrNull() }
    )
)

// ================================================================
//  2. Package Info
// ================================================================
private val PackageInfoReviseItems = listOf(
    ProfileReviseHeader { stringResource(id = R.string.app_info) },
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.version_name) },
        value = { versionName },
        onValueChange = { copy(versionName = it) },
    ),
    ProfileReviseEditor.TextNumber(
        label = { stringResource(id = R.string.version_code) },
        value = { versionCode },
        onValueChange = { copy(versionCode = it) },
        stringToNumber = { it.toIntOrNull() }
    )
)

// ================================================================
//  3. Resource Configuration / Identity / Timezone
// ================================================================
private val ResourceConfigReviseItems = listOf(
    ProfileReviseHeader { stringResource(id = R.string.resource_configuration) },
    ProfileReviseEditor.Enum(
        label = { stringResource(id = R.string.language) },
        value = { language },
        options = { language },
        onValueClear = { copy(language = null) },
        onSelectedChange = { copy(language = it.value) }
    ),
    ProfileReviseEditor.BooleanEnum(
        label = { stringResource(id = R.string.night_mode) },
        value = { nightMode },
        onValueClear = { copy(nightMode = null) },
        onSelectedChange = { copy(nightMode = it.value) }
    ),
    ProfileReviseEditor.TextNumber(
        label = { stringResource(id = R.string.density_dpi) },
        value = { densityDpi },
        onValueChange = { copy(densityDpi = it) },
        stringToNumber = { it.toIntOrNull() }
    ),
    ProfileReviseEditor.TextNumber(
        label = { stringResource(id = R.string.font_scale) },
        value = { fontScale },
        onValueChange = { copy(fontScale = it) },
        stringToNumber = { it.toFloatOrNull() }
    )
)

// ================================================================
//  4. Location + Base Station
// ================================================================
private val LocationReviseItems = listOf(
    ProfileReviseHeader { stringResource(id = R.string.location) },
    ProfileReviseEditor.TextNumber(
        label = { stringResource(id = R.string.longitude) },
        value = { longitude },
        onValueChange = { copy(longitude = it) },
        stringToNumber = { it.toDoubleOrNull() }
    ),
    ProfileReviseEditor.TextNumber(
        label = { stringResource(id = R.string.latitude) },
        value = { latitude },
        onValueChange = { copy(latitude = it) },
        stringToNumber = { it.toDoubleOrNull() }
    )
)

private val BaseStationReviseItems = listOf(
    ProfileReviseHeader { stringResource(id = R.string.base_station) },
    ProfileReviseEditor.TextNumber(
        label = { "Cid" },
        value = { cid },
        onValueChange = { copy(cid = it) },
        stringToNumber = { it.toLongOrNull() }
    ),
    ProfileReviseEditor.TextNumber(
        label = { "Lac/Tac" },
        value = { lac },
        onValueChange = { copy(lac = it) },
        stringToNumber = { it.toIntOrNull() }
    ),
    ProfileReviseEditor.TextNumber(
        label = { "Pci" },
        value = { pci },
        onValueChange = { copy(pci = it) },
        stringToNumber = { it.toIntOrNull() }
    )
)

// ================================================================
//  5. Network + Network Extension (Android 14+ 新接口)
// ================================================================
private val NetworkReviseItems = listOf(
    ProfileReviseHeader { stringResource(id = R.string.network_info) },
    ProfileReviseEditor.TextNumber(
        label = { stringResource(id = R.string.network_type) },
        value = { networkType },
        onValueChange = { copy(networkType = it) },
        stringToNumber = { it.toIntOrNull() },
        suggestRepo = inject<NetworkType>(NetworkType::class.java).value
    ),
    ProfileReviseEditor.TextNumber(
        label = { stringResource(id = R.string.mobile_network_type) },
        value = { mobileNetType },
        onValueChange = { copy(mobileNetType = it) },
        stringToNumber = { it.toIntOrNull() },
        suggestRepo = MobileNetworkTypeRepo
    )
)

private val NetworkExtReviseItems = listOf(
    ProfileReviseHeader { stringResource(id = R.string.network_ext) },
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.ip_address) },
        value = { ipAddress },
        onValueChange = { copy(ipAddress = it) }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.http_proxy_host) },
        value = { httpProxyHost },
        onValueChange = { copy(httpProxyHost = it) }
    ),
    ProfileReviseEditor.TextNumber(
        label = { stringResource(id = R.string.http_proxy_port) },
        value = { httpProxyPort },
        onValueChange = { copy(httpProxyPort = it) },
        stringToNumber = { it.toIntOrNull() }
    ),
    ProfileReviseEditor.TextNumber(
        label = { stringResource(id = R.string.net_meteredness) },
        value = { netMeteredness },
        onValueChange = { copy(netMeteredness = it) },
        stringToNumber = { it.toIntOrNull() }
    )
)

// ================================================================
//  6. Identity (SSAID / IMEI / MSISDN / DRMiD 等)
// ================================================================
private val IdentityReviseItems = listOf(
    ProfileReviseHeader { stringResource(id = R.string.identity) },
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.ssaid) },
        value = { ssaid },
        onValueChange = { copy(ssaid = it) },
        suggestRepo = AndroidIdRandomRepo
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.build_serial_method) },
        value = { buildSerial },
        onValueChange = { copy(buildSerial = it) }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.ethernet_mac) },
        value = { ethernetMac },
        onValueChange = { copy(ethernetMac = it) }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.drm_device_id) },
        value = { drmDeviceId },
        onValueChange = { copy(drmDeviceId = it) }
    )
)

private val TelephonyReviseItems = listOf(
    ProfileReviseHeader { stringResource(id = R.string.telephony_identifiers) },
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.imei) },
        value = { imei },
        onValueChange = { copy(imei = it) }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.imei2) },
        value = { imei2 },
        onValueChange = { copy(imei2 = it) }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.imsi) },
        value = { imsi },
        onValueChange = { copy(imsi = it) }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.line1_number) },
        value = { line1Number },
        onValueChange = { copy(line1Number = it) }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.sim_serial_number) },
        value = { simSerialNumber },
        onValueChange = { copy(simSerialNumber = it) }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.meid) },
        value = { meid },
        onValueChange = { copy(meid = it) }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.sim_country_iso) },
        value = { simCountryIso },
        onValueChange = { copy(simCountryIso = it) }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.network_country_iso) },
        value = { networkCountryIso },
        onValueChange = { copy(networkCountryIso = it) }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.network_operator) },
        value = { networkOperator },
        onValueChange = { copy(networkOperator = it) }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.sim_operator_name) },
        value = { simOperatorName },
        onValueChange = { copy(simOperatorName = it) }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.network_operator_name) },
        value = { networkOperatorName },
        onValueChange = { copy(networkOperatorName = it) }
    ),
    ProfileReviseEditor.TextNumber(
        label = { stringResource(id = R.string.device_id_type) },
        value = { deviceIdType },
        onValueChange = { copy(deviceIdType = it) },
        stringToNumber = { it.toIntOrNull() }
    )
)

// ================================================================
//  7. 账号/谷歌账号
// ================================================================
private val AccountReviseItems = listOf(
    ProfileReviseHeader { stringResource(id = R.string.account_info) },
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.google_account) },
        value = { googleAccount },
        onValueChange = { copy(googleAccount = it) }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.account_name) },
        value = { accountName },
        onValueChange = { copy(accountName = it) }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.account_type) },
        value = { accountType },
        onValueChange = { copy(accountType = it) }
    ),
    ProfileReviseEditor.BooleanEnum(
        label = { stringResource(id = R.string.disable_accounts) },
        value = { if (disableAccounts) true else null },
        onValueClear = { copy(disableAccounts = false) },
        onSelectedChange = { copy(disableAccounts = it.value) }
    )
)

// ================================================================
//  8. 蓝牙
// ================================================================
private val BluetoothReviseItems = listOf(
    ProfileReviseHeader { stringResource(id = R.string.bluetooth_info) },
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.bluetooth_name) },
        value = { bluetoothName },
        onValueChange = { copy(bluetoothName = it) }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.bluetooth_address) },
        value = { bluetoothAddress },
        onValueChange = { copy(bluetoothAddress = it) }
    ),
    ProfileReviseEditor.BooleanEnum(
        label = { stringResource(id = R.string.disable_bluetooth) },
        value = { if (disableBluetooth) true else null },
        onValueClear = { copy(disableBluetooth = false) },
        onSelectedChange = { copy(disableBluetooth = it.value) }
    )
)

// ================================================================
//  9. 广告ID (GAID/OAID/VAID/AAID)
// ================================================================
private val AdvertisingReviseItems = listOf(
    ProfileReviseHeader { stringResource(id = R.string.advertising_info) },
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.advertising_id) },
        value = { advertisingId },
        onValueChange = { copy(advertisingId = it) }
    ),
    ProfileReviseEditor.BooleanEnum(
        label = { stringResource(id = R.string.lat_enabled) },
        value = { latEnabled },
        onValueClear = { copy(latEnabled = null) },
        onSelectedChange = { copy(latEnabled = it.value) }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.oaid) },
        value = { oaid },
        onValueChange = { copy(oaid = it) }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.vaid) },
        value = { vaid },
        onValueChange = { copy(vaid = it) }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.aaid) },
        value = { aaid },
        onValueChange = { copy(aaid = it) }
    )
)

// ================================================================
//  10. 传感器
// ================================================================
private val SensorReviseItems = listOf(
    ProfileReviseHeader { stringResource(id = R.string.sensor_info) },
    ProfileReviseEditor.BooleanEnum(
        label = { stringResource(id = R.string.disable_accelerometer) },
        value = { if (disableAccelerometer) true else null },
        onValueClear = { copy(disableAccelerometer = false) },
        onSelectedChange = { copy(disableAccelerometer = it.value) }
    ),
    ProfileReviseEditor.BooleanEnum(
        label = { stringResource(id = R.string.disable_gyroscope) },
        value = { if (disableGyroscope) true else null },
        onValueClear = { copy(disableGyroscope = false) },
        onSelectedChange = { copy(disableGyroscope = it.value) }
    ),
    ProfileReviseEditor.BooleanEnum(
        label = { stringResource(id = R.string.disable_magnetometer) },
        value = { if (disableMagnetometer) true else null },
        onValueClear = { copy(disableMagnetometer = false) },
        onSelectedChange = { copy(disableMagnetometer = it.value) }
    ),
    ProfileReviseEditor.BooleanEnum(
        label = { stringResource(id = R.string.disable_step_counter) },
        value = { if (disableStepCounter) true else null },
        onValueClear = { copy(disableStepCounter = false) },
        onSelectedChange = { copy(disableStepCounter = it.value) }
    ),
    ProfileReviseEditor.BooleanEnum(
        label = { stringResource(id = R.string.disable_all_sensors) },
        value = { if (disableAllSensors) true else null },
        onValueClear = { copy(disableAllSensors = false) },
        onSelectedChange = { copy(disableAllSensors = it.value) }
    )
)

// ================================================================
//  11. USB 与调试
// ================================================================
private val UsbDebugReviseItems = listOf(
    ProfileReviseHeader { stringResource(id = R.string.usb_debug_info) },
    ProfileReviseEditor.BooleanEnum(
        label = { stringResource(id = R.string.usb_state_charging_only) },
        value = { if (usbStateChargingOnly) true else null },
        onValueClear = { copy(usbStateChargingOnly = false) },
        onSelectedChange = { copy(usbStateChargingOnly = it.value) }
    ),
    ProfileReviseEditor.BooleanEnum(
        label = { stringResource(id = R.string.disable_usb_debugging) },
        value = { if (disableUsbDebugging) true else null },
        onValueClear = { copy(disableUsbDebugging = false) },
        onSelectedChange = { copy(disableUsbDebugging = it.value) }
    )
)

// ================================================================
//  12. 用户空间 (UserHandle/UserManager，Android 14+ 工作资料检测项)
// ================================================================
private val UserSpaceReviseItems = listOf(
    ProfileReviseHeader { stringResource(id = R.string.user_info) },
    ProfileReviseEditor.TextNumber(
        label = { stringResource(id = R.string.user_id) },
        value = { userId },
        onValueChange = { copy(userId = it) },
        stringToNumber = { it.toIntOrNull() }
    ),
    ProfileReviseEditor.TextNumber(
        label = { stringResource(id = R.string.user_serial_number) },
        value = { userSerialNumber },
        onValueChange = { copy(userSerialNumber = it) },
        stringToNumber = { it.toLongOrNull() }
    )
)

// ================================================================
//  13. 壁纸
// ================================================================
private val WallpaperReviseItems = listOf(
    ProfileReviseHeader { stringResource(id = R.string.wallpaper_info) },
    ProfileReviseEditor.TextNumber(
        label = { stringResource(id = R.string.wallpaper_min_width) },
        value = { wallpaperDesiredMinimumWidth },
        onValueChange = { copy(wallpaperDesiredMinimumWidth = it) },
        stringToNumber = { it.toIntOrNull() }
    ),
    ProfileReviseEditor.TextNumber(
        label = { stringResource(id = R.string.wallpaper_min_height) },
        value = { wallpaperDesiredMinimumHeight },
        onValueChange = { copy(wallpaperDesiredMinimumHeight = it) },
        stringToNumber = { it.toIntOrNull() }
    )
)

// ================================================================
//  14. 强制截屏（绕过FLAG_SECURE）
// ================================================================
private val ScreenshotForceReviseItems = listOf(
    ProfileReviseHeader { stringResource(id = R.string.screenshot_force_info) },
    ProfileReviseEditor.BooleanEnum(
        label = { stringResource(id = R.string.force_screenshot_enabled) },
        value = { if (forceScreenshotEnabled) true else null },
        onValueClear = { copy(forceScreenshotEnabled = false) },
        onSelectedChange = { copy(forceScreenshotEnabled = it.value) }
    )
)

// ================================================================
//  15. 录屏/投屏检测 (MediaProjection)
// ================================================================
private val MediaProjectionReviseItems = listOf(
    ProfileReviseHeader { stringResource(id = R.string.mediaprojection_info) },
    ProfileReviseEditor.BooleanEnum(
        label = { stringResource(id = R.string.disable_media_projection) },
        value = { if (disableMediaProjection) true else null },
        onValueClear = { copy(disableMediaProjection = false) },
        onSelectedChange = { copy(disableMediaProjection = it.value) }
    ),
    ProfileReviseEditor.TextNumber(
        label = { stringResource(id = R.string.fake_virtual_display_count) },
        value = { fakeVirtualDisplayCount },
        onValueChange = { copy(fakeVirtualDisplayCount = it) },
        stringToNumber = { it.toIntOrNull() }
    )
)

// ================================================================
//  16. 剪贴板
// ================================================================
private val ClipboardReviseItems = listOf(
    ProfileReviseHeader { stringResource(id = R.string.clipboard_info) },
    ProfileReviseEditor.BooleanEnum(
        label = { stringResource(id = R.string.disable_clipboard_read) },
        value = { if (disableClipboardRead) true else null },
        onValueClear = { copy(disableClipboardRead = false) },
        onSelectedChange = { copy(disableClipboardRead = it.value) }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.fake_clipboard_text) },
        value = { fakeClipboardText },
        onValueChange = { copy(fakeClipboardText = it) }
    )
)

// ================================================================
//  17. /proc Native 指纹
// ================================================================
private val ProcFsReviseItems = listOf(
    ProfileReviseHeader { stringResource(id = R.string.procfs_info) },
    ProfileReviseEditor.BooleanEnum(
        label = { stringResource(id = R.string.disable_proc_fingerprint) },
        value = { if (disableProcFingerprint) true else null },
        onValueClear = { copy(disableProcFingerprint = false) },
        onSelectedChange = { copy(disableProcFingerprint = it.value) }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.fake_cpu_model_name) },
        value = { fakeCpuModelName },
        onValueChange = { copy(fakeCpuModelName = it) }
    ),
    ProfileReviseEditor.TextNumber(
        label = { stringResource(id = R.string.fake_mem_total_kb) },
        value = { fakeMemTotalKb },
        onValueChange = { copy(fakeMemTotalKb = it) },
        stringToNumber = { it.toIntOrNull() }
    )
)

// ================================================================
//  18. Build Expect 一致性校验 (Android 15 PARTIAL_DEEP_CHECK)
// ================================================================
private val BuildExpectReviseItems = listOf(
    ProfileReviseHeader { stringResource(id = R.string.build_expect_info) },
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.expect_fingerprint) },
        value = { expectFingerprint },
        onValueChange = { copy(expectFingerprint = it) }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.expect_hardware) },
        value = { expectHardware },
        onValueChange = { copy(expectHardware = it) }
    )
)

// ================================================================
//  19. GPU / OpenGL 指纹
// ================================================================
private val GpuFingerprintReviseItems = listOf(
    ProfileReviseHeader { stringResource(id = R.string.gpu_info) },
    ProfileReviseEditor.BooleanEnum(
        label = { stringResource(id = R.string.disable_gpu_fingerprint) },
        value = { if (disableGpuFingerprint) true else null },
        onValueClear = { copy(disableGpuFingerprint = false) },
        onSelectedChange = { copy(disableGpuFingerprint = it.value) }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.gl_renderer) },
        value = { glRenderer },
        onValueChange = { copy(glRenderer = it) }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.gl_vendor) },
        value = { glVendor },
        onValueChange = { copy(glVendor = it) }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.gl_version) },
        value = { glVersion },
        onValueChange = { copy(glVersion = it) }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.egl_vendor) },
        value = { eglVendor },
        onValueChange = { copy(eglVendor = it) }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.egl_version) },
        value = { eglVersion },
        onValueChange = { copy(eglVersion = it) }
    )
)

// ================================================================
//  20. 安装来源
// ================================================================
private val InstallSourceReviseItems = listOf(
    ProfileReviseHeader { stringResource(id = R.string.install_source_info) },
    ProfileReviseEditor.BooleanEnum(
        label = { stringResource(id = R.string.fake_install_from_gplay) },
        value = { if (fakeInstalledFromGooglePlay) true else null },
        onValueClear = { copy(fakeInstalledFromGooglePlay = false) },
        onSelectedChange = { copy(fakeInstalledFromGooglePlay = it.value) }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.installer_package) },
        value = { installerPackage },
        onValueChange = { copy(installerPackage = it) }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.fake_install_initiating) },
        value = { fakeInstallSourceInitiatingPackage },
        onValueChange = { copy(fakeInstallSourceInitiatingPackage = it) }
    ),
    ProfileReviseEditor.Text(
        label = { stringResource(id = R.string.fake_install_originating) },
        value = { fakeInstallSourceOriginatingPackage },
        onValueChange = { copy(fakeInstallSourceOriginatingPackage = it) }
    )
)

// ================================================================
//  21. 运行时长 / 开机时间
// ================================================================
private val UptimeReviseItems = listOf(
    ProfileReviseHeader { stringResource(id = R.string.uptime_info) },
    ProfileReviseEditor.BooleanEnum(
        label = { stringResource(id = R.string.fake_uptime_one_week_old) },
        value = { if (fakeUptimeOneWeekOld) true else null },
        onValueClear = { copy(fakeUptimeOneWeekOld = false) },
        onSelectedChange = { copy(fakeUptimeOneWeekOld = it.value) }
    ),
    ProfileReviseEditor.TextNumber(
        label = { stringResource(id = R.string.device_uptime_offset_ms) },
        value = { deviceUptimeOffsetMs },
        onValueChange = { copy(deviceUptimeOffsetMs = it) },
        stringToNumber = { it.toLongOrNull() }
    ),
    ProfileReviseEditor.TextNumber(
        label = { stringResource(id = R.string.fake_boot_completed_ts_ms) },
        value = { fakeBootCompletedTimestampMs },
        onValueChange = { copy(fakeBootCompletedTimestampMs = it) },
        stringToNumber = { it.toLongOrNull() }
    )
)

// ================================================================
//  汇总导出（UI读取此列表进行渲染）
// ================================================================
val ProfilesReviseItemsDef = listOf(
    // 1. Properties / Hardware / Build.VERSION
    PropertiesReviseItems,
    HardwarePropertiesReviseItems,
    BuildVersionReviseItems,
    // 2. 网络
    NetworkReviseItems,
    NetworkExtReviseItems,
    // 3. 定位与基站
    LocationReviseItems,
    BaseStationReviseItems,
    // 4. 应用信息 & 资源配置
    PackageInfoReviseItems,
    ResourceConfigReviseItems,
    // 5. 标识符 & 电话标识符
    IdentityReviseItems,
    TelephonyReviseItems,
    // 6. 账号（谷歌账号）
    AccountReviseItems,
    // 7. 蓝牙
    BluetoothReviseItems,
    // 8. 广告ID
    AdvertisingReviseItems,
    // 9. 传感器
    SensorReviseItems,
    // 10. USB 与调试
    UsbDebugReviseItems,
    // 11. 用户空间
    UserSpaceReviseItems,
    // 12. 壁纸
    WallpaperReviseItems,
    // 14. 强制截屏
    ScreenshotForceReviseItems,
    // 15. 录屏/投屏检测
    MediaProjectionReviseItems,
    // 16. 剪贴板
    ClipboardReviseItems,
    // 17. /proc Native 指纹
    ProcFsReviseItems,
    // 18. Build Expect (Android 15+)
    BuildExpectReviseItems,
    // 19. GPU / OpenGL 指纹
    GpuFingerprintReviseItems,
    // 20. 安装来源
    InstallSourceReviseItems,
    // 21. 运行时长 / 开机时间
    UptimeReviseItems,
).flatten()

private fun Profiles.properties(function: PropertiesProfile.() -> PropertiesProfile) =
    copy(properties = properties.function())
