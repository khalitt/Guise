package com.houvven.guise.hook.profile

import com.houvven.guise.hook.profile.item.PropertiesProfile
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json


@Serializable
data class HookProfiles(
    @Transient
    val packageName: String? = null,
    val properties: PropertiesProfile = PropertiesProfile.Empty,

    // Package Info
    val versionName: String? = null,
    val versionCode: Int? = null,

    // Resource Configuration
    val language: String? = null,
    val densityDpi: Int? = null,
    val fontScale: Float? = null,
    val nightMode: Boolean? = null,

    // Network
    val networkType: Int? = null,
    val mobileNetType: Int? = null,
    val simOperator: String? = null,

    val wifiSsid: String? = null,
    val wifiBssid: String? = null,
    val wifiMac: String? = null,

    // Base station
    val cid: Long? = null,
    val lac: Int? = null,
    val pci: Int? = null,

    // Location
    /** 经度 */
    val longitude: Double? = null,
    /** 维度 */
    val latitude: Double? = null,
    /** 返回空的WiFi信息  */
    val disableWifiLocation: Boolean = false,
    /** 返回空的基站信息 */
    val disableCellLocation: Boolean = false,


    val ssaid: String? = null,

    val timezone: String? = null,

    // ================================================================
    //  新增区域（Android 14 / 15 / 16 + 账号/蓝牙/传感器/标识符）
    // ================================================================

    // ---- 1. 账号/谷歌账号伪装 ----
    /** 伪装已登录的谷歌账号邮箱 (如 user@gmail.com)，会在 AccountManager.getAccountsByType("com.google") 返回 */
    val googleAccount: String? = null,
    /** 伪装所有账号列表的通用账号（格式 "name:type"，可多组逗号分隔） */
    val accountName: String? = null,
    val accountType: String? = null,
    /** 返回空账号列表，彻底隐藏真实账号 */
    val disableAccounts: Boolean = false,

    // ---- 2. 电话标识符（TelephonyManager IMEI/IMSI/MSISDN/ICCID等）----
    /** IMEI (卡槽1)，对应 getImei(0) / getDeviceId() */
    val imei: String? = null,
    /** IMEI (卡槽2) Android 14+ 双SIM接口 */
    val imei2: String? = null,
    /** IMSI getSubscriberId() */
    val imsi: String? = null,
    /** 手机号码 getLine1Number() */
    val line1Number: String? = null,
    /** SIM卡序列号 ICCID getSimSerialNumber() */
    val simSerialNumber: String? = null,
    /** MEID（CDMA机型） */
    val meid: String? = null,
    /** getSimCountryIso() 如 "cn" "us" */
    val simCountryIso: String? = null,
    /** getNetworkCountryIso() */
    val networkCountryIso: String? = null,
    /** getNetworkOperator() MNC+MCC */
    val networkOperator: String? = null,
    /** getSimOperatorName() / getNetworkOperatorName() */
    val simOperatorName: String? = null,
    val networkOperatorName: String? = null,
    /** Android 14+ getDeviceIdType() 接口 */
    val deviceIdType: Int? = null,

    // ---- 3. 蓝牙伪装 ----
    val bluetoothName: String? = null,
    val bluetoothAddress: String? = null,
    /** 彻底禁用蓝牙，返回蓝牙状态关闭 */
    val disableBluetooth: Boolean = false,

    // ---- 4. 广告ID / OAID / GAID ----
    /** 伪装 Google Play Services 的 AdvertisingId */
    val advertisingId: String? = null,
    /** 限制广告追踪状态 (LAT) true=限制 */
    val latEnabled: Boolean? = null,
    /** 国内设备 OAID */
    val oaid: String? = null,
    /** VAID / AAID */
    val vaid: String? = null,
    val aaid: String? = null,

    // ---- 5. 其他硬件标识符 ----
    /** Media DRM Widevine ID / Device ID */
    val drmDeviceId: String? = null,
    /** MAC地址 (以太网/其他接口) */
    val ethernetMac: String? = null,
    /** Build.getSerial() */
    val buildSerial: String? = null,

    // ---- 6. 传感器伪装 ----
    /** 禁用所有加速度计 (重力感应)，防止通过传感器特征指纹识别设备 */
    val disableAccelerometer: Boolean = false,
    /** 禁用陀螺仪 */
    val disableGyroscope: Boolean = false,
    /** 禁用磁力计 */
    val disableMagnetometer: Boolean = false,
    /** 禁用所有传感器 */
    val disableAllSensors: Boolean = false,
    /** 屏蔽步数计数器 */
    val disableStepCounter: Boolean = false,

    // ---- 7. USB / 连接伪装 ----
    /** 返回 USB 未连接 / 充电模式 */
    val usbStateChargingOnly: Boolean = false,
    /** 屏蔽 USB MIDI / 开发者调试等接口 */
    val disableUsbDebugging: Boolean = false,

    // ---- 8. 系统用户 / 用户空间伪装 (Android 14+ 工作资料检测项) ----
    /** getSerialNumber() 的用户序列号 */
    val userSerialNumber: Long? = null,
    /** 伪装成非主用户（工作资料用户ID） */
    val userId: Int? = null,

    // ---- 9. 网络扩展（IP地址、HTTP代理等）----
    /** 伪装 IPv4 地址 */
    val ipAddress: String? = null,
    /** 伪装 HTTP 代理主机 */
    val httpProxyHost: String? = null,
    val httpProxyPort: Int? = null,
    /** Android 14+ getMeteredness() 网络计费接口 */
    val netMeteredness: Int? = null,

    // ---- 10. Wallpaper / WallpaperID (环境检测项) ----
    val wallpaperDesiredMinimumWidth: Int? = null,
    val wallpaperDesiredMinimumHeight: Int? = null,

    // ---- 11. 强制截屏（绕过FLAG_SECURE）----
    /**
     * 当App通过 Window.setFlags(FLAG_SECURE) 禁止截屏/录屏时，
     * 开启此开关会强制移除该标志，允许系统级截屏/录屏/投屏软件获取画面
     *   - 效果等同于禁用银行/视频APP的"防止截图"
     *   - 同时绕过 Android 14+ 的 PrivacyMode (窗口隐私模式)
     *   - 同时绕过 SurfaceControl 的 CAPTURE_BLACK_OUT 显示保护
     */
    val forceScreenshotEnabled: Boolean = false,

    // ================================================================
    //  进阶反作弊增强（建议方向全部实现）
    // ================================================================

    // ---- 12. 屏幕录制/共享 / MediaProjection 检测项 ----
    /**
     * 伪装"当前没有任何屏幕录制/投屏会话正在进行"
     *   - 反作弊会读取 MediaProjectionManager + DisplayManager
     *   - 某些金融APP检测录屏/投屏后会禁止操作，开启后返回"无会话"
     */
    val disableMediaProjection: Boolean = false,
    /** 额外伪装 VirtualDisplay 回调 count=0 (Android 14+ VirtualDisplayId 接口) */
    val fakeVirtualDisplayCount: Int? = null,

    // ---- 13. 剪贴板 (Clipboard) ----
    /** 清空剪贴板：APP读取 getPrimaryClip() 时返回空，防止跨APP粘贴复制 */
    val disableClipboardRead: Boolean = false,
    /** 自定义剪贴板内容（纯文本），默认不配置则清空 */
    val fakeClipboardText: String? = null,

    // ---- 14. /proc 文件系统 (Native 反作弊指纹) ----
    /**
     * Hook libc fopen/openat 对 /proc/cpuinfo /proc/meminfo /proc/self/maps
     * 做字符串替换，屏蔽：
     *   - 虚拟 SoC 型号 / 主板 / xposed 模块路径
     *   - 高权限进程映射
     */
    val disableProcFingerprint: Boolean = false,
    /** /proc/cpuinfo 第一行 model name 覆盖 */
    val fakeCpuModelName: String? = null,
    /** /proc/meminfo MemTotal 覆盖 (单位 kB) */
    val fakeMemTotalKb: Int? = null,

    // ---- 15. Android 15+ CTS 深度指纹 PARTIAL_DEEP_CHECK ----
    /** Build.FINGERPRINT vs ro.build.expect.fingerprint 一致性校验的期望指纹 */
    val expectFingerprint: String? = null,
    /** Build.HARDWARE 对应 ro.build.expect.hardware */
    val expectHardware: String? = null,

    // ---- 16. GPU / GL 渲染器指纹 ----
    /** 伪装 GL_RENDERER (OpenGL 渲染器型号，如 Adreno (TM) 640) */
    val glRenderer: String? = null,
    /** 伪装 GL_VENDOR (如 Qualcomm Inc.) */
    val glVendor: String? = null,
    /** 伪装 GL_VERSION (如 OpenGL ES 3.2 V@464.0) */
    val glVersion: String? = null,
    /** 伪装 EGL_VERSION / EGL_VENDOR / EGL_EXTENSIONS */
    val eglVendor: String? = null,
    val eglVersion: String? = null,
    /** 直接 hook glGetString 全接口返回空/自定义，彻底破坏GL指纹 */
    val disableGpuFingerprint: Boolean = false,

    // ---- 17. 安装来源 / InstallSource ----
    /** 伪装 ApplicationInfo.packageInstallerName (安装来源包名) */
    val installerPackage: String? = null,
    /**
     * 伪装 Android 14+ InstallSourceInfo
     * 默认内置：com.android.vending (Google Play Store) / com.huawei.appmarket / com.xiaomi.market
     * 可通过 installerPackage 字段自由选择
     */
    val fakeInstallSourceInitiatingPackage: String? = null,
    val fakeInstallSourceOriginatingPackage: String? = null,
    /**
     * PackageManager.GET_META_DATA 的 INSTALLER_PACKAGE_NAME 返回值也会一同被替换；
     * 配置 true 则默认伪装为 Google Play（值为 com.android.vending ）
     */
    val fakeInstalledFromGooglePlay: Boolean = false,

    // ---- 18. 系统运行时长 / 开机时间 (防止"刚刷机/新设备"指纹) ----
    /**
     * 对 SystemClock.elapsedRealtime() / uptimeMillis() / currentThreadTimeMillis()
     * 叠加一个固定偏移（毫秒），让"系统已运行多久"看起来像一个稳定运行的老设备
     * （注意：仅正向偏移，避免负值引发问题）
     */
    val deviceUptimeOffsetMs: Long? = null,
    /** 对 BootCompleted 广播的 /proc/stat 开机时间戳做伪装 */
    val fakeBootCompletedTimestampMs: Long? = null,
    /**
     * 简化开关：一键让"已运行时长"增长 7 天（168小时）
     * 比手动配置 offset 方便，新设备指纹立即可隐藏
     */
    val fakeUptimeOneWeekOld: Boolean = false,

) : Profile {

    val mcc = simOperator?.substring(0, 3)
    val mnc = simOperator?.substring(3)
    val tac = lac

    val isLocationAvailable get() = listOf(latitude, longitude).any { it != null }

    override val isAvailable: Boolean
        get() = this != Empty.copy(packageName = this.packageName)

    fun toJsonStr(): String {
        return Json.encodeToString(serializer(), this)
    }

    companion object {
        @JvmStatic
        val Empty = HookProfiles()

        @JvmStatic
        val Debug = HookProfiles(
            properties = PropertiesProfile(
                brand = "Xiaomi",
                model = "M2105K81C",
                characteristics = "tablet"
            )
        )

        fun fromJsonStr(json: String): HookProfiles {
            return runCatching { Json.decodeFromString(serializer(), json) }.getOrDefault(Empty)
        }

    }
}
