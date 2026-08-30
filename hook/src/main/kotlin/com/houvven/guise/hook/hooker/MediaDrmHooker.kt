package com.houvven.guise.hook.hooker

import com.highcapable.betterandroid.system.extension.tool.SystemVersion
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.current
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.java.ByteArrayClass
import com.highcapable.yukihookapi.hook.type.java.StringClass
import com.houvven.guise.hook.profile.HookProfiles
import com.houvven.guise.hook.util.type.MediaDrmClass

/**
 * Media DRM (Widevine L1/L3) 设备唯一ID 伪装 Hooker
 * 反作弊会调用 MediaDrm.getPropertyByteArray("deviceUniqueId") 取到的字节做指纹
 * 或 getPropertyString("vendorId" / "description" / ... )
 *
 * Android 14+ 引入了更强制的 drmSession 安全等级校验，
 * 我们不直接hook getKeyRequest 以免破坏DRM播放，只针对取ID相关接口。
 */
internal class MediaDrmHooker(private val profile: HookProfiles) : YukiBaseHooker() {

    override fun onHook() {
        MediaDrmClass ?: return
        profile.drmDeviceId?.let { idStr ->
            // deviceUniqueId 返回 byte[]，把字符串转为UTF-8字节数组或HEX解码
            val bytes = idStr.takeIf { it.matches(Regex("[0-9A-Fa-f]+")) }
                ?.chunked(2)?.map { it.toInt(16).toByte() }?.toByteArray()
                ?: idStr.toByteArray(Charsets.UTF_8)

            MediaDrmClass!!.method {
                name("getPropertyByteArray")
                param(StringClass)
            }.ignored().hook().before {
                val key = args.first() as String
                if (key.equals("deviceUniqueId", ignoreCase = true)) {
                    result = bytes
                }
            }
        }
    }
}
