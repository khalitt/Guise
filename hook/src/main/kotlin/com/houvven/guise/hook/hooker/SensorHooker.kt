package com.houvven.guise.hook.hooker

import android.hardware.Sensor
import android.hardware.SensorManager
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.houvven.guise.hook.profile.HookProfiles
import com.houvven.guise.hook.util.type.SensorManagerClass

/**
 * 传感器伪装 Hooker
 * 用途：
 *  1. 禁用某类型传感器，让 getDefaultSensor / getSensorList 返回空/无此设备
 *     （可阻止通过传感器噪声、陀螺仪漂零值等进行设备指纹识别，近年反作弊场景大量使用）
 *  2. 屏蔽步数计数器等运动传感器
 */
internal class SensorHooker(private val profile: HookProfiles) : YukiBaseHooker() {

    override fun onHook() {
        SensorManagerClass ?: return
        hookDefaultSensor()
        hookSensorList()
    }

    /** 判断某个sensor type是否应该被禁用 */
    private fun shouldDisableSensor(type: Int): Boolean {
        if (profile.disableAllSensors) return true
        return when (type) {
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_ACCELEROMETER_UNCALIBRATED,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_LINEAR_ACCELERATION -> profile.disableAccelerometer
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> profile.disableGyroscope
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> profile.disableMagnetometer
            Sensor.TYPE_STEP_COUNTER,
            Sensor.TYPE_STEP_DETECTOR -> profile.disableStepCounter
            else -> false
        }
    }

    private fun hookDefaultSensor() {
        SensorManagerClass!!.method {
            name("getDefaultSensor")
            param(IntType)
        }.ignored().hook().before {
            val type = args.first() as Int
            if (shouldDisableSensor(type)) result = null
        }
        // 三参数版本 getDefaultSensor(type, wakeUp)
        runCatching {
            SensorManagerClass.method {
                name("getDefaultSensor")
                param(IntType, IntType)
            }
        }.getOrNull()?.ignored()?.hook()?.before {
            val type = args.first() as Int
            if (shouldDisableSensor(type)) result = null
        }
    }

    private fun hookSensorList() {
        SensorManagerClass!!.method {
            name("getSensorList")
            param(IntType)
        }.ignored().hook().after {
            @Suppress("UNCHECKED_CAST")
            val list = result as? MutableList<Sensor> ?: return@after
            val iter = list.iterator()
            while (iter.hasNext()) {
                val s = iter.next()
                if (shouldDisableSensor(s.type)) iter.remove()
            }
            // 当type为 Sensor.TYPE_ALL 也返回过滤后的列表；若配置了disableAllSensors直接清空
            if (profile.disableAllSensors) list.clear()
            result = list
        }
    }
}
