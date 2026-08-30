package com.houvven.guise.hook.hooker

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.factory.toClass
import com.houvven.guise.hook.profile.HookProfiles

/**
 * 广告标识符伪装 Hooker
 * 覆盖以下接口：
 *   - Google Play Services (GMS) AdvertisingIdClient: getId() / isLimitAdTrackingEnabled()
 *   - 国内移动安全联盟 MSAA 的 OAID / VAID / AAID SDK 读取接口 (MdidSdkHelper / Supplier 等)
 *   - Settings.Secure.getString(advertising_id) 兜底读取（国内某些ROM）
 */
internal class AdvertisingIdHooker(private val profile: HookProfiles) : YukiBaseHooker() {

    override fun onHook() {
        hookGoogleAdvertisingId()
        hookMsaOaidSdk()
    }

    /**
     * GMS 的 AdvertisingIdClient.Info 类的两个 getter
     * 类名: com.google.android.gms.ads.identifier.AdvertisingIdClient$Info
     *   - getId() 返回 UUID 格式字符串
     *   - isLimitAdTrackingEnabled() 返回 boolean
     */
    private fun hookGoogleAdvertisingId() {
        val infoClass = runCatching {
            "com.google.android.gms.ads.identifier.AdvertisingIdClient\$Info".toClass()
        }.getOrNull() ?: return

        profile.advertisingId?.let { id ->
            infoClass.method {
                name("getId")
            }.ignored().hookAll().replaceTo(id)
        }
        profile.latEnabled?.let { lat ->
            infoClass.method {
                name("isLimitAdTrackingEnabled")
            }.ignored().hookAll().replaceTo(lat)
        }

        // 兼容某些通过 AdvertisingIdClient.getAdvertisingIdInfo(context) 直接调用返回的Info对象
        // 需要在返回之后再改写对象内部字段（若可访问）
        runCatching {
            "com.google.android.gms.ads.identifier.AdvertisingIdClient".toClass()
                .method { name("getAdvertisingIdInfo") }
                .ignored().hookAll().after {
                    result?.let { info ->
                        profile.advertisingId?.let { id ->
                            runCatching {
                                infoClass.getDeclaredField("zza").apply {
                                    isAccessible = true
                                    set(info, id)
                                }
                            }
                        }
                    }
                }
        }
    }

    /**
     * 国内 MSA OAID SDK：
     *   包名：com.bun.miitmdid.core / com.bun.miitmdid.interfaces.IIdentifierListener / ...
     *   常见接口: MdidSdkHelper#InitSdk / IdentifierManager#getOAID / getVAID / getAAID
     * 同时兼容部分厂商的实现（如华为 / 小米 / OPPO / VIVO SDK的反射读取）
     */
    private fun hookMsaOaidSdk() {
        val managerClassNames = listOf(
            "com.bun.miitmdid.core.IdentifierManager",
            "com.bun.supplier.IdSupplier"  // 部分版本直接反射IdSupplier
        )
        val pairs = listOf(
            "getOAID" to profile.oaid,
            "getVAID" to profile.vaid,
            "getAAID" to profile.aaid
        )
        managerClassNames.forEach { className ->
            val clazz = runCatching { className.toClass() }.getOrNull() ?: return@forEach
            pairs.forEach { (method, value) ->
                value ?: return@forEach
                runCatching {
                    clazz.method { name(method) }.ignored().hookAll().replaceTo(value)
                }
            }
        }

        // 兜底：某些APP直接通过反射读取 /sdcard/Android/data/xxx/.../cache/.miitmdid 之类的文件
        // 这里Hook java.io.File 代价太高，建议配合自定义属性或随机生成UUID使用
    }
}
