package com.houvven.guise.hook.hooker

import android.bluetooth.BluetoothAdapter
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.houvven.guise.hook.profile.HookProfiles
import com.houvven.guise.hook.util.type.BluetoothAdapterClass
import com.houvven.guise.hook.util.type.BluetoothManagerClass

/**
 * 蓝牙标识符伪装 Hooker
 * 覆盖：蓝牙名称、蓝牙MAC地址、状态(STATE_OFF禁用蓝牙)
 * 同时兼容 BluetoothAdapter.getDefaultAdapter() 和 BluetoothManager.getAdapter() 两套接口
 */
internal class BluetoothHooker(private val profile: HookProfiles) : YukiBaseHooker() {

    override fun onHook() {
        BluetoothAdapterClass ?: return
        hookBluetoothState()
        hookBluetoothName()
        hookBluetoothAddress()
    }

    /** 禁用蓝牙，返回STATE_OFF；或保证getState返回ON避免检测差异 */
    private fun hookBluetoothState() {
        if (profile.disableBluetooth) {
            BluetoothAdapterClass!!.method {
                name("getState")
            }.ignored().hookAll().replaceTo(BluetoothAdapter.STATE_OFF)
            BluetoothAdapterClass.method {
                name("isEnabled")
            }.ignored().hookAll().replaceToFalse()
            BluetoothManagerClass?.method {
                name("getAdapter")
            }?.ignored()?.hookAll()?.replaceTo(null)
        }
    }

    private fun hookBluetoothName() {
        profile.bluetoothName?.let { name ->
            // 1. 读取getName()返回值
            BluetoothAdapterClass!!.method {
                name("getName")
            }.ignored().hookAll().replaceTo(name)
            // 2. 写入后，把BluetoothAdapter实例里的mName字段也替换
            BluetoothAdapterClass.method {
                name("setName")
            }.ignored().hookAll().after {
                instance?.let { inst ->
                    BluetoothAdapterClass.field {
                        name("mName")
                    }.ignored().get(inst)?.set(inst, name)
                }
            }
            // 3. getDefaultAdapter()得到对象后替换字段
            BluetoothAdapterClass.method {
                name("getDefaultAdapter")
            }.ignored().hookAll().after {
                result?.let { adapter ->
                    runCatching {
                        BluetoothAdapterClass.field {
                            name("mName")
                        }.ignored().get(adapter)?.set(adapter, name)
                    }
                }
            }
        }
    }

    private fun hookBluetoothAddress() {
        profile.bluetoothAddress?.let { addr ->
            BluetoothAdapterClass!!.method {
                name("getAddress")
            }.ignored().hookAll().replaceTo(addr)
            // 同上，替换实例字段
            BluetoothAdapterClass.method {
                name("getDefaultAdapter")
            }.ignored().hookAll().after {
                result?.let { adapter ->
                    runCatching {
                        BluetoothAdapterClass.field {
                            name("mAddress")
                        }.ignored().get(adapter)?.set(adapter, addr)
                    }
                }
            }
        }
    }
}
