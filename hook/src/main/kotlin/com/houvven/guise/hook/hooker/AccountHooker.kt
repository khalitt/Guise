package com.houvven.guise.hook.hooker

import android.accounts.Account
import com.highcapable.betterandroid.system.extension.tool.SystemVersion
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.java.StringClass
import com.houvven.guise.hook.profile.HookProfiles
import com.houvven.guise.hook.util.type.AccountClass
import com.houvven.guise.hook.util.type.AccountManagerClass
import java.lang.reflect.Array

/**
 * 账号/谷歌账号伪装 Hooker
 * 目标接口:
 *   AccountManager.getAccounts()
 *   AccountManager.getAccountsByType(String type)
 *   AccountManager.getAccountsByTypeAndFeatures(...)
 *   AccountManager.getAccountsAsUser(...)  // Android 14+ 用户空间接口
 *   AccountManager.hasFeatures(...)
 */
internal class AccountHooker(private val profile: HookProfiles) : YukiBaseHooker() {

    override fun onHook() {
        AccountManagerClass ?: return
        hookGetAccounts()
        hookGetAccountsByType()
        if (SystemVersion.has(SystemVersion.JB_MR1)) {
            hookGetAccountsAsUser()
        }
    }

    /** 返回构造的假账号数组，如果开启了disableAccounts则返回空数组 */
    private fun buildAccountsByType(filterType: String?): Any {
        val accounts = mutableListOf<Pair<String, String>>() // name to type
        when {
            profile.disableAccounts -> Unit
            else -> {
                profile.googleAccount?.takeIf { filterType == null || filterType == "com.google" }
                    ?.let { accounts.add(it to "com.google") }
                profile.accountName?.let { name ->
                    val t = profile.accountType ?: "com.google"
                    if (filterType == null || filterType == t) accounts.add(name to t)
                }
            }
        }
        val arr = Array.newInstance(AccountClass, accounts.size)
        accounts.forEachIndexed { i, (name, type) ->
            val ctor = AccountClass.getConstructor(StringClass, StringClass)
            Array.set(arr, i, ctor.newInstance(name, type))
        }
        return arr
    }

    private fun hookGetAccounts() {
        listOf("getAccounts", "getAccountsByVisibility").forEach { name ->
            AccountManagerClass!!.method {
                name(name)
            }.ignored().hookAll().after {
                result = buildAccountsByType(null)
            }
        }
    }

    private fun hookGetAccountsByType() {
        listOf("getAccountsByType", "getAccountsByTypeAndFeatures").forEach { name ->
            AccountManagerClass!!.method {
                name(name)
            }.ignored().hookAll().after {
                val type = args.firstOrNull() as? String
                result = buildAccountsByType(type)
            }
        }
    }

    /** Android 14+ 引入的多用户接口，会根据UserHandle取账号 */
    private fun hookGetAccountsAsUser() {
        listOfNotNull(
            runCatching {
                AccountManagerClass!!.method {
                    name("getAccountsAsUser")
                }
            }.getOrNull()
        ).forEach {
            it.hookAll().after {
                val type = args.getOrNull(1) as? String
                result = buildAccountsByType(type)
            }
        }
    }
}
