package com.houvven.guise.hook.hooker

import android.os.Build
import android.util.Log
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.classOf
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.android.BuildClass
import com.houvven.guise.hook.profile.item.PropertiesProfile
import com.houvven.guise.hook.util.type.BuildVersionClass
import com.houvven.guise.hook.util.type.BuildVersionCodesClass
import com.houvven.guise.hook.util.type.SystemPropertiesClass

private const val TAG = "PropertiesHook"

internal class PropertiesHooker(private  val profile: PropertiesProfile) :
    YukiBaseHooker() {

    private val options = mutableListOf<PropertiesHookOption>()

    init {
        options.addAll(buildHookOption())
    }

    override fun onHook() {
        // Hook the value of the field in the Build class
        options.forEach { option ->
            val (value, fieldName, _, type) = option
            if (!fieldName.isNullOrBlank()) {
                when (type) {
                    BuildPropAscription.BUILD -> BuildClass
                    BuildPropAscription.VERSION -> BuildVersionClass
                    BuildPropAscription.VERSION_CODES -> BuildVersionCodesClass
                    null -> null
                }?.run {
                    Log.d(TAG, "hook `Build` class 's static field: $fieldName, value: $value")
                    field {
                        name = fieldName
                        modifiers {
                            isStatic
                        }
                    }.ignored().give()?.set(null, value)
                }
            }
        }

        // Hook Build.getSerial() 单独处理
        profile.serial?.let { serial ->
            BuildClass.method {
                name = "getSerial"
            }.ignored().hookAll().replaceTo(serial)
        }

        // Hook the value of the system properties
        val associate = options.associate { it.propertiesKey to it.value }
        SystemPropertiesClass.method {
            name = "get"
        }.hookAll {
            before {
                val index = args.indexOfFirst { associate.containsKey(it) }
                if (index != -1) {
                    val key = args[index]
                    Log.d(TAG, "hook system properties: $key, value: ${associate[key]}")
                    result = associate[key]
                }
            }
            after {
                if (args.any { associate.containsKey(it) }) {
                    Log.d(TAG, "get system properties: ${args.first()}, value: $result")
                }
            }
        }
    }


    private fun buildHookOption() = profile.run {
        val result = mutableListOf(
            // ---------------- 原有属性 ----------------
            PropertiesHookOption(
                value = brand,
                fieldName = "BRAND",
                propertiesKey = "ro.product.brand",
                type = BuildPropAscription.BUILD
            ),
            PropertiesHookOption(
                value = manufacturer,
                fieldName = "MANUFACTURER",
                propertiesKey = "ro.product.manufacturer",
                type = BuildPropAscription.BUILD
            ),
            PropertiesHookOption(
                value = model,
                fieldName = "MODEL",
                propertiesKey = "ro.product.model",
                type = BuildPropAscription.BUILD
            ),
            PropertiesHookOption(
                value = product,
                fieldName = "PRODUCT",
                propertiesKey = "ro.product.name",
                type = BuildPropAscription.BUILD
            ),
            PropertiesHookOption(
                value = device,
                fieldName = "DEVICE",
                propertiesKey = "ro.product.device",
                type = BuildPropAscription.BUILD
            ),
            PropertiesHookOption(
                value = displayId,
                fieldName = "DISPLAY",
                propertiesKey = "ro.build.display.id",
                type = BuildPropAscription.BUILD,
            ),
            PropertiesHookOption(
                value = fingerprint,
                type = BuildPropAscription.BUILD,
                propertiesKey = "ro.build.fingerprint"
            ),
            PropertiesHookOption(
                value = fingerprint,
                fieldName = "FINGERPRINT",
                propertiesKey = "ro.build.fingerprint",
                type = BuildPropAscription.BUILD
            ),
            PropertiesHookOption(
                value = characteristics,
                propertiesKey = "ro.build.characteristics"
            ),
            // ---------------- 新增硬件属性 ----------------
            PropertiesHookOption(
                value = hardware,
                fieldName = "HARDWARE",
                propertiesKey = "ro.hardware",
                type = BuildPropAscription.BUILD
            ),
            PropertiesHookOption(
                value = board,
                fieldName = "BOARD",
                propertiesKey = "ro.product.board",
                type = BuildPropAscription.BUILD
            ),
            PropertiesHookOption(
                value = bootloader,
                fieldName = "BOOTLOADER",
                propertiesKey = "ro.bootloader",
                type = BuildPropAscription.BUILD
            ),
            PropertiesHookOption(
                value = radioVersion,
                fieldName = "RADIO",
                propertiesKey = "gsm.version.baseband",
                type = BuildPropAscription.BUILD
            ),
            PropertiesHookOption(
                value = serial,
                fieldName = "SERIAL",
                propertiesKey = "ro.serialno",
                type = BuildPropAscription.BUILD
            ),
            // Android 12+ 新增
            PropertiesHookOption(
                value = socModel,
                fieldName = "SOC_MODEL",
                propertiesKey = "ro.soc.model",
                type = BuildPropAscription.BUILD
            ),
            PropertiesHookOption(
                value = socManufacturer,
                fieldName = "SOC_MANUFACTURER",
                propertiesKey = "ro.soc.manufacturer",
                type = BuildPropAscription.BUILD
            ),
            // Android 14+ 新增
            PropertiesHookOption(
                value = odmSku,
                fieldName = "ODM_SKU",
                propertiesKey = "ro.product.odm.sku",
                type = BuildPropAscription.BUILD
            ),
            PropertiesHookOption(
                value = sku,
                fieldName = "SKU",
                propertiesKey = "ro.boot.hardware.sku",
                type = BuildPropAscription.BUILD
            ),
            // ---------------- Build.VERSION 版本号伪装 ----------------
            PropertiesHookOption(
                value = release,
                fieldName = "RELEASE",
                propertiesKey = "ro.build.version.release",
                type = BuildPropAscription.VERSION
            ),
            PropertiesHookOption(
                value = sdkInt,
                fieldName = "SDK_INT",
                propertiesKey = "ro.build.version.sdk",
                type = BuildPropAscription.VERSION
            ),
            PropertiesHookOption(
                value = previewSdkInt,
                fieldName = "PREVIEW_SDK_INT",
                propertiesKey = "ro.build.version.preview_sdk",
                type = BuildPropAscription.VERSION
            ),
            PropertiesHookOption(
                value = incremental,
                fieldName = "INCREMENTAL",
                propertiesKey = "ro.build.version.incremental",
                type = BuildPropAscription.VERSION
            ),
            PropertiesHookOption(
                value = codename,
                fieldName = "CODENAME",
                propertiesKey = "ro.build.version.codename",
                type = BuildPropAscription.VERSION
            ),
            PropertiesHookOption(
                value = securityPatch,
                fieldName = "SECURITY_PATCH",
                propertiesKey = "ro.build.version.security_patch",
                type = BuildPropAscription.VERSION
            ),
            PropertiesHookOption(
                value = mediaPerformanceClass,
                fieldName = "MEDIA_PERFORMANCE_CLASS",
                propertiesKey = "ro.odm.build.media_performance_class",
                type = BuildPropAscription.VERSION
            ),
            // ---------------- VBMeta / Partition ----------------
            PropertiesHookOption(
                value = vbmetaDigest,
                propertiesKey = "ro.boot.vbmeta.digest"
            )
        )
        result.filter { it.value != null }.let {
            it + customProperties.map { (key, value) ->
                PropertiesHookOption(
                    value = value,
                    propertiesKey = key
                )
            }
        }
    }


    enum class BuildPropAscription {
        VERSION,
        VERSION_CODES,
        BUILD
    }

    data class PropertiesHookOption(
        val value: Any?,
        /**
         * The field name of [android.os.Build] class. such as `BRAND`, `MODEL`, `PRODUCT`, `DEVICE`
         */
        val fieldName: String? = null,
        /**
         * The property key of system properties. such as `ro.product.brand`, `ro.product.model`, `ro.product.name`, `ro.product.device`
         */
        val propertiesKey: String,
        /**
         *Used to mark the attribution of attributes, for example [android.os.Build], [android.os.Build.VERSION], [android.os.Build.VERSION_CODES]
         */
        val type: BuildPropAscription? = null
    )
}
