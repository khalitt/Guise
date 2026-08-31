package com.houvven.guise.hook.hooker

import android.os.Build
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.highcapable.yukihookapi.hook.type.java.StringClass
import com.houvven.guise.hook.profile.HookProfiles
import com.houvven.guise.hook.util.type.SubscriptionInfoClass
import com.houvven.guise.hook.util.type.SubscriptionManagerClass
import com.houvven.guise.hook.util.type.TelephonyManagerClass

/**
 * 电话标识符伪装 Hooker
 * 覆盖 IMEI / IMEI2 / IMSI / MSISDN(手机号) / ICCID / MEID /
 *       SIM国家代码 / 网络运营商 / 设备ID类型 等接口
 *
 * 注意：Android 10 之后普通应用无法通过 getDeviceId / getImei 获取到真实值，
 * 但 Xposed Hook 对特权进程(如系统服务、设备管理app)仍可伪装。
 */
internal class TelephonyInfoHooker(private val profile: HookProfiles) : YukiBaseHooker() {

    override fun onHook() {
        hookDeviceIdentifiers()
        hookSubscriberInfo()
        hookSimInfo()
        hookCountryAndOperator()
        hookSubscriptionInfo()
    }

    // ---- Device ID 系列 ----
    private fun hookDeviceIdentifiers() {
        // 旧版 getDeviceId() / getDeviceId(int slot)
        profile.imei?.let { imei ->
            TelephonyManagerClass.method {
                name("getDeviceId")
            }.ignored().hookAll().replaceTo(imei)
        }

        // Android 8+ getImei() / getImei(int slotIndex)，双卡区分
        profile.imei?.let { imei ->
            TelephonyManagerClass.method {
                name("getImei")
            }.ignored().hookAll().after {
                // 若只配置了imei但有slot参数，根据slot区分imei1/imei2
                val slot = args.firstOrNull() as? Int
                result = if (slot == 1 && !profile.imei2.isNullOrBlank()) {
                    profile.imei2
                } else {
                    imei
                }
            }
        }
        profile.imei2?.let { imei2 ->
            // 显式处理 getImei(1)
            TelephonyManagerClass.method {
                name("getImei")
                param(IntType)
            }.ignored().hook().before {
                val slot = args.first() as Int
                if (slot == 1) result = imei2
            }
        }

        // MEID (CDMA机型)
        profile.meid?.let { meid ->
            TelephonyManagerClass.method {
                name("getMeid")
            }.ignored().hookAll().replaceTo(meid)
        }

        // Android 14+ getDeviceIdType()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            profile.deviceIdType?.let { type ->
                TelephonyManagerClass.method {
                    name("getDeviceIdType")
                }.ignored().hookAll().replaceTo(type)
            }
        }
    }

    // ---- IMSI / MSISDN ----
    private fun hookSubscriberInfo() {
        profile.imsi?.let { imsi ->
            TelephonyManagerClass.method {
                name("getSubscriberId")
            }.ignored().hookAll().replaceTo(imsi)
        }
        profile.line1Number?.let { number ->
            TelephonyManagerClass.method {
                name("getLine1Number")
            }.ignored().hookAll().replaceTo(number)
            // Android 12+ getLine1Number(int subscriptionId)
            TelephonyManagerClass.method {
                name("getLine1Number")
                param(IntType)
            }.ignored().hook().replaceTo(number)
        }
    }

    // ---- SIM卡信息 ----
    private fun hookSimInfo() {
        profile.simSerialNumber?.let { iccid ->
            TelephonyManagerClass.method {
                name("getSimSerialNumber")
            }.ignored().hookAll().replaceTo(iccid)
        }
        profile.simOperatorName?.let {
            TelephonyManagerClass.method {
                name("getSimOperatorName")
            }.ignored().hookAll().replaceTo(it)
        }
        profile.simOperator?.let { // mcc+mnc
            TelephonyManagerClass.method {
                name("getSimOperator")
            }.ignored().hookAll().replaceTo(it)
        }
    }

    // ---- 国家代码 & 运营商 ----
    private fun hookCountryAndOperator() {
        profile.simCountryIso?.let {
            TelephonyManagerClass.method {
                name("getSimCountryIso")
            }.ignored().hookAll().replaceTo(it)
        }
        profile.networkCountryIso?.let {
            TelephonyManagerClass.method {
                name("getNetworkCountryIso")
            }.ignored().hookAll().replaceTo(it)
        }
        profile.networkOperator?.let {
            TelephonyManagerClass.method {
                name("getNetworkOperator")
            }.ignored().hookAll().replaceTo(it)
        }
        profile.networkOperatorName?.let {
            TelephonyManagerClass.method {
                name("getNetworkOperatorName")
            }.ignored().hookAll().replaceTo(it)
        }
    }

    /**
     * SubscriptionInfo 包含更细粒度的 SIM卡标识符字段
     * 如 getIccId() / getMccString() / getMncString() / getNumber()
     */
    private fun hookSubscriptionInfo() {
        listOfNotNull(SubscriptionInfoClass, SubscriptionManagerClass)
        if (profile.simSerialNumber == null && profile.line1Number == null
            && profile.mcc == null && profile.mnc == null) return
        runCatching {
            SubscriptionInfoClass!!.method {
                name("getIccId")
            }.ignored().hookAll().apply {
                profile.simSerialNumber?.let { replaceTo(it) }
            }
            SubscriptionInfoClass.method {
                name("getNumber")
            }.ignored().hookAll().apply {
                profile.line1Number?.let { replaceTo(it) }
            }
            // SubscriptionManager.getAllSubscriptionInfoList() 返回的列表也通过getter读取
        }
    }
}
