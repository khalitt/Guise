package com.houvven.guise.hook.profile.item

import com.houvven.guise.hook.profile.Profile
import kotlinx.serialization.Serializable

@Serializable
data class PropertiesProfile(
    val brand: String? = null,
    val manufacturer: String? = brand,
    val model: String? = null,
    val product: String? = null,
    val device: String? = null,
    val displayId: String? = null,
    val fingerprint: String? = null,

    val characteristics: String? = null,

    // ---- 新增：硬件/主板/引导相关属性 ----
    val hardware: String? = null,
    val board: String? = null,
    val bootloader: String? = null,
    val radioVersion: String? = null,
    val serial: String? = null,
    val socModel: String? = null,
    val socManufacturer: String? = null,
    val odmSku: String? = null,
    val sku: String? = null,

    // ---- 新增：Build.VERSION 版本号伪装 (用于伪装不同Android版本) ----
    val release: String? = null,          // Build.VERSION.RELEASE (例 "14", "15", "16")
    val sdkInt: Int? = null,              // Build.VERSION.SDK_INT
    val previewSdkInt: Int? = null,       // Build.VERSION.PREVIEW_SDK_INT (Android 14+)
    val incremental: String? = null,      // Build.VERSION.INCREMENTAL
    val codename: String? = null,         // Build.VERSION.CODENAME
    val securityPatch: String? = null,    // Build.VERSION.SECURITY_PATCH
    val mediaPerformanceClass: Int? = null, // Build.VERSION.MEDIA_PERFORMANCE_CLASS (API 31+)

    // ---- 新增：AB分区/VBMeta/启动时间属性 (Android 14+ 新增检测) ----
    val systemOnOtherPartitionsSupported: Boolean? = null,
    val vbmetaDigest: String? = null,

    // 自定义属性 (兜底)
    val customProperties: Map<String, String> = emptyMap()
) : Profile {

    override val isAvailable: Boolean = this != Empty

    companion object {
        val Empty = PropertiesProfile()
    }
}
