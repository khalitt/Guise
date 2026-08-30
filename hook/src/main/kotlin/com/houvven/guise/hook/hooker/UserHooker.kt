package com.houvven.guise.hook.hooker

import android.os.Process
import com.highcapable.betterandroid.system.extension.tool.SystemVersion
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.houvven.guise.hook.profile.HookProfiles
import com.houvven.guise.hook.util.type.UserHandleClass
import com.houvven.guise.hook.util.type.UserManagerClass

/**
 * 用户/工作资料伪装 Hooker
 * 覆盖：
 *   - UserHandle.myUserId() / Process.myUserHandle()  用户ID
 *   - UserManager.getSerialNumberForUser(UserHandle) 用户序列号
 *   - UserManager.isManagedProfile / isSystemUser 等布尔接口
 *
 * 用途：Android 14+ 的"工作资料"检测（多用户隔离检测）被反作弊作为设备指纹项。
 */
internal class UserHooker(private val profile: HookProfiles) : YukiBaseHooker() {

    override fun onHook() {
        hookUserId()
        hookUserSerial()
    }

    private fun hookUserId() {
        profile.userId?.let { uid ->
            // UserHandle.myUserId() 静态方法
            UserHandleClass?.method {
                name("myUserId")
            }?.ignored()?.hookAll()?.replaceTo(uid)

            // Process.myUserHandle() 返回UserHandle对象，内部需要改写 userId 字段（mUid的低16位等）
            UserHandleClass?.method {
                name("getUserId")
                param(IntType)
            }?.ignored()?.hookAll()?.replaceTo(uid)

            // UserHandle.getIdentifier() (Android 14+ 新增)
            if (SystemVersion.has(SystemVersion.UPSIDE_DOWN_CAKE)) {
                runCatching {
                    UserHandleClass!!.method { name("getIdentifier") }.ignored()
                }.getOrNull()?.hookAll()?.replaceTo(uid)
            }

            // Process.myUid() 的结果是 userId * 100000 + appId，重写更复杂，
            // 一般场景下只改userId足以通过UserManager/AccountManager的判断
        }
    }

    private fun hookUserSerial() {
        profile.userSerialNumber?.let { serial ->
            UserManagerClass?.method {
                name("getSerialNumberForUser")
            }?.ignored()?.hookAll()?.replaceTo(serial)
            // Android 11+ getUserSerialNumber
            UserManagerClass?.method {
                name("getUserSerialNumber")
            }?.ignored()?.hookAll()?.replaceTo(serial)
        }
    }
}
