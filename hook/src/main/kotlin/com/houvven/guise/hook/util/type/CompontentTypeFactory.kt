package com.houvven.guise.hook.util.type

import android.accounts.AccountManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ContentResolver
import android.hardware.SensorManager
import android.location.GnssStatus
import android.location.GpsStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import android.media.MediaDrm
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import android.telephony.CellIdentity
import android.telephony.CellIdentityCdma
import android.telephony.CellIdentityGsm
import android.telephony.CellIdentityLte
import android.telephony.CellIdentityNr
import android.telephony.CellIdentityTdscdma
import android.telephony.CellIdentityWcdma
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import com.highcapable.yukihookapi.hook.factory.classOf
import com.highcapable.yukihookapi.hook.factory.toClass
import java.util.concurrent.Executor


val SystemPropertiesClass = "android.os.SystemProperties".toClass()

val LocationClass = classOf<Location>()

val LocationManagerClass = classOf<LocationManager>()

val LocationRequestClass = classOf<LocationRequest>()

val LocationListenerClass = classOf<LocationListener>()

val ExecutorClass = classOf<Executor>()

val TelephonyManagerClass = classOf<TelephonyManager>()

val SubscriptionManagerClass = runCatching { classOf<SubscriptionManager>() }.getOrNull()

val SubscriptionInfoClass = runCatching { classOf<SubscriptionInfo>() }.getOrNull()

val CellIdentityClass = classOf<CellIdentity>()

@RequiresApi(Build.VERSION_CODES.Q)
val CellIdentityNrClass = classOf<CellIdentityNr>()

val CellIdentityLteClass = classOf<CellIdentityLte>()

val CellIdentityGsmClass = classOf<CellIdentityGsm>()

val CellIdentityCdma = classOf<CellIdentityCdma>()

val CellIdentityWcdmaClass = classOf<CellIdentityWcdma>()

val CellIdentityTdscdmaClass = classOf<CellIdentityTdscdma>()

val GnssStatusClass = classOf<GnssStatus>()

val GpsStatusClass = classOf<GpsStatus>()

val ConnectivityManagerClass = classOf<ConnectivityManager>()

val NetworkClass = runCatching { classOf<Network>() }.getOrNull()

val NetworkCapabilitiesClass = runCatching { classOf<NetworkCapabilities>() }.getOrNull()

val LinkPropertiesClass = runCatching { classOf<LinkProperties>() }.getOrNull()

val WifiManagerClass  = classOf<WifiManager>()

val WifiInfoClass = classOf<WifiInfo>()

// ---- 新增：账号/蓝牙/传感器/User ----
val AccountManagerClass = runCatching { classOf<AccountManager>() }.getOrNull()
val AccountClass = "android.accounts.Account".toClass()

val BluetoothAdapterClass = runCatching { classOf<BluetoothAdapter>() }.getOrNull()
val BluetoothManagerClass = runCatching { classOf<BluetoothManager>() }.getOrNull()

val SensorManagerClass = runCatching { classOf<SensorManager>() }.getOrNull()
val SensorClass = "android.hardware.Sensor".toClass()

val UserHandleClass = runCatching { classOf<UserHandle>() }.getOrNull()
val UserManagerClass = runCatching { classOf<UserManager>() }.getOrNull()

val MediaDrmClass = runCatching { classOf<MediaDrm>() }.getOrNull()

val ContentResolverClass = classOf<ContentResolver>()

val BuildClass = classOf<Build>()
val BuildVersionClass = classOf<Build.VERSION>()
val BuildVersionCodesClass = classOf<Build.VERSION_CODES>()
