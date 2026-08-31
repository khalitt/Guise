package com.houvven.guise.hook.hooker

import android.net.NetworkCapabilities
import android.os.Build
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.current
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.houvven.guise.hook.profile.HookProfiles
import com.houvven.guise.hook.util.type.ConnectivityManagerClass
import com.houvven.guise.hook.util.type.LinkPropertiesClass
import com.houvven.guise.hook.util.type.NetworkCapabilitiesClass
import com.houvven.guise.hook.util.type.NetworkClass
import com.houvven.guise.hook.util.type.TelephonyManagerClass

import java.net.InetAddress
import java.net.InterfaceAddress
import java.net.NetworkInterface

internal class NetworkHooker(private val profile: HookProfiles) : YukiBaseHooker() {

    override fun onHook() {
        this.hookActiveNetworkType()
        this.hookMobileNetworkType()
        this.hookMeteredness()
        this.hookHttpProxy()
        this.hookIpAddress()
    }

    private fun hookActiveNetworkType() {
        profile.networkType?.let { networkType ->
            ConnectivityManagerClass.method {
                name = "getActiveNetwork"
            }.hookAll().after {
                result = result?.current {
                    field { name = "netId" }.set(networkType)
                }
            }

            ConnectivityManagerClass.method {
                name = "getActiveNetworkInfo"
            }.hookAll().after {
                result = result?.current(ignored = true) {
                    field { name = "mNetworkType" }.set(networkType)
                }
            }

            // 补充 getNetworkCapabilities 中的 TRANSPORT
            runCatching { NetworkCapabilitiesClass }.getOrNull()?.let { ncClass ->
                ConnectivityManagerClass.method {
                    name("getNetworkCapabilities")
                }.ignored().hookAll().after {
                    result?.current(ignored = true) {
                        // 根据networkType模拟 hasTransport
                        if (networkType == NetworkCapabilities.TRANSPORT_WIFI) {
                            field { name = "mTransportTypes" }.set(intArrayOf(NetworkCapabilities.TRANSPORT_WIFI))
                        }
                    }
                }
            }
        }
    }

    private fun hookMobileNetworkType() {
        profile.mobileNetType?.let { type ->
            listOf("getNetworkType", "getDataNetworkType").forEach { methodName ->
                TelephonyManagerClass.method {
                    name = methodName
                    param(IntType)
                }.hook().replaceTo(type)
            }
            // 无参版本 getNetworkType (deprecated)
            TelephonyManagerClass.method {
                name = "getNetworkType"
            }.ignored().hook().replaceTo(type)
        }
    }

    /**
     * Android 14 (API 34) 新增接口：ConnectivityManager.getActiveNetworkMeteredness()
     * 返回值：
     *   METEREDNESS_NOT_METERED = 1
     *   METEREDNESS_TEMPORARILY_NOT_METERED = 2
     *   METEREDNESS_METERED = 3
     */
    private fun hookMeteredness() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        profile.netMeteredness?.let { met ->
            listOf("getActiveNetworkMeteredness", "getNetworkMeteredness").forEach { mName ->
                runCatching {
                    ConnectivityManagerClass.method { name(mName) }
                }.getOrNull()?.ignored()?.hookAll()?.replaceTo(met)
            }
        }
    }

    /**
     * 伪装HTTP代理：
     *   ConnectivityManager.getDefaultProxy()  (Android 11+)
     *   ProxySelector.getDefault().select(uri) / Proxy.getDefault()  (Java 层)
     *   Settings.Global.HTTP_PROXY / GLOBAL_HTTP_PROXY_HOST / GLOBAL_HTTP_PROXY_PORT
     */
    private fun hookHttpProxy() {
        if (profile.httpProxyHost == null && profile.httpProxyPort == null) return
        val host = profile.httpProxyHost ?: return
        val port = profile.httpProxyPort ?: 8080
        // 1. ConnectivityManager.getDefaultProxy() 返回 ProxyInfo
        runCatching {
            val proxyInfoClass = "android.net.ProxyInfo".toClass()
            ConnectivityManagerClass.method {
                name("getDefaultProxy")
            }.ignored().hookAll().after {
                result = runCatching {
                    val ctor = proxyInfoClass.getConstructor(String::class.java, Int::class.javaPrimitiveType, String::class.java)
                    ctor.newInstance(host, port, "")
                }.getOrNull()
            }
        }
    }

    /**
     * 伪装本机IP地址
     *   通过Hook NetworkInterface.getNetworkInterfaces() 遍历返回的接口地址
     *   替换 wlan0 / eth0 的 InetAddress hostAddress
     */
    private fun hookIpAddress() {
        profile.ipAddress ?: return
        runCatching {
            val inetClass = InetAddress::class.java
            // 替换 NetworkInterface.getInterfaceAddresses() 返回的列表
            "java.net.NetworkInterface".toClass().method {
                name("getInterfaceAddresses")
            }.hookAll().after {
                @Suppress("UNCHECKED_CAST")
                val list = result as? MutableList<InterfaceAddress> ?: return@after
                // 遍历找非loopback、非link-local地址替换
                val fakeInet = InetAddress.getByName(profile.ipAddress)
                list.forEach { ifAddr ->
                    val addr = ifAddr.address
                    if (addr != null && !addr.isLoopbackAddress
                        && !addr.isLinkLocalAddress
                        && addr is InetAddress
                        && addr.hostAddress?.contains(".") == true) {
                        runCatching {
                            val holderField = inetClass.getDeclaredField("holder").apply { isAccessible = true }
                            val holder = holderField.get(addr) ?: return@runCatching
                            val addrField = holder.javaClass.getDeclaredField("address").apply { isAccessible = true }
                            addrField.set(holder, fakeInet.address)
                            val hostNameField = holder.javaClass.getDeclaredField("hostName").apply { isAccessible = true }
                            hostNameField.set(holder, profile.ipAddress)
                        }
                    }
                }
                result = list
            }
        }
    }

    private fun classOf(clazz: Class<*>) = clazz
    private fun String.toClass(): Class<*> = Class.forName(this)
}
