package com.houvven.guise.hook.hooker

import android.content.pm.ApplicationInfo
import android.content.pm.InstallSourceInfo
import android.os.Build
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.factory.toClass
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.highcapable.yukihookapi.hook.type.java.StringClass
import com.houvven.guise.hook.profile.HookProfiles

/**
 * 安装来源 / InstallSource 伪装 Hooker
 *
 * 反作弊常见检查：
 *   - PackageManager.getInstallerPackageName(pkg)    传统接口 (deprecated in API 30)
 *   - ApplicationInfo.packageInstallerName (API 30+)
 *   - PackageManager.getInstallSourceInfo(pkg)    (API 30+)
 *   - PackageInstaller.SessionParams 新接口 (API 34+ initiatingPackage / originatingPackage / attestation)
 *
 * 我们支持：
 *   - fakeInstalledFromGooglePlay=true  →  一键伪装来自 Google Play (com.android.vending)
 *   - installerPackage=xxx              →  自定义安装来源包名
 *   - fakeInstallSourceInitiating/Originating → 安卓14+ InstallSourceInfo 字段
 */
internal class InstallSourceHooker(private val profile: HookProfiles) : YukiBaseHooker() {

    private val targetPkg: String by lazy {
        when {
            profile.fakeInstalledFromGooglePlay -> GOOGLE_PLAY_PKG
            profile.installerPackage?.isNotBlank() == true -> profile.installerPackage!!
            else -> ""
        }
    }
    private val initiatingPkg: String by lazy {
        profile.fakeInstallSourceInitiatingPackage ?: targetPkg
    }
    private val originatingPkg: String by lazy {
        profile.fakeInstallSourceOriginatingPackage ?: targetPkg
    }

    private val enabled: Boolean by lazy {
        profile.fakeInstalledFromGooglePlay
                || profile.installerPackage?.isNotBlank() == true
                || profile.fakeInstallSourceInitiatingPackage?.isNotBlank() == true
                || profile.fakeInstallSourceOriginatingPackage?.isNotBlank() == true
    }

    override fun onHook() {
        if (!enabled) return
        hookPackageManagerInstaller()
        hookApplicationInfo()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            hookInstallSourceInfo()
        }
    }

    /** 传统接口：PackageManager.getInstallerPackageName */
    private fun hookPackageManagerInstaller() {
        val pmClass = "android.content.pm.PackageManager".toClass()
        runCatching {
            pmClass.method {
                name("getInstallerPackageName")
                param(StringClass)
            }.hookAll().replaceTo(targetPkg)
        }
        // 新版 getInstallSourceInfo(pkg) 单独处理
    }

    /** ApplicationInfo.packageInstallerName (API 30+) 这个字段是只读的，一般通过构造ApplicationInfo的field设置 */
    private fun hookApplicationInfo() {
        runCatching {
            ApplicationInfo::class.java.getDeclaredField("packageInstallerName").apply {
                isAccessible = true
            }
        }.onSuccess { field ->
            // 命中点：PackageParser.generateApplicationInfo 返回的ApplicationInfo实例，我们无法精确hook
            // 这里做兜底：任何 getPackageInfo 返回后扫描
            val pmClass = "android.content.pm.PackageManager".toClass()
            listOf(
                "getPackageInfo",
                "getApplicationInfo"
            ).forEach { mName ->
                runCatching {
                    pmClass.method {
                        name(mName)
                        param(StringClass, IntType)
                    }.hookAll().after {
                        val result = result ?: return@after
                        when {
                            result is ApplicationInfo -> field.set(result, targetPkg)
                            result is android.content.pm.PackageInfo -> result.applicationInfo?.let {
                                field.set(it, targetPkg)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Android 11+ InstallSourceInfo
     *   public String getInstallingPackageName()  安装发起者 (deprecated)
     *   public String getInitiatingPackageName()  Android 14 新
     *   public String getOriginatingPackageName()
     *
     * 替换策略：Hook getInstallSourceInfo 返回后改写它的内部字段（因为InstallSourceInfo构造有hide限制）
     */
    private fun hookInstallSourceInfo() {
        val pmClass = "android.content.pm.PackageManager".toClass()
        val isiClass = runCatching { InstallSourceInfo::class.java }.getOrNull() ?: return
        runCatching {
            pmClass.method {
                name("getInstallSourceInfo")
                param(StringClass)
            }.hookAll().after {
                val info = result as? InstallSourceInfo ?: return@after
                // 直接替换内部String字段，InstallSourceInfo 不可变类字段名会根据ROM略有差异，尽量覆盖
                listOf(
                    "mInstallingPackageName" to initiatingPkg,
                    "mInitiatingPackageName" to initiatingPkg,
                    "mOriginatingPackageName" to originatingPkg,
                    "installingPackageName" to initiatingPkg,
                    "initiatingPackageName" to initiatingPkg,
                    "originatingPackageName" to originatingPkg
                ).forEach { (fname, value) ->
                    runCatching {
                        isiClass.getDeclaredField(fname).apply {
                            isAccessible = true
                            set(info, value)
                        }
                    }
                }
                result = info
            }
        }
    }

    companion object {
        private const val GOOGLE_PLAY_PKG = "com.android.vending"
    }
}
