package com.houvven.guise.hook.hooker

import android.content.ClipData
import android.content.ClipDescription
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.factory.toClass
import com.hhighcapable.yukihookapi.hook.type.java.IntType
import com.houvven.guise.hook.profile.HookProfiles

/**
 * 剪贴板 (ClipboardManager) 伪装 Hooker
 *
 * 反作弊常见手段：
 *   - 读取剪贴板看有没有"脚本/辅助"类的口令/粘贴内容
 *   - 不同APP之间通过剪贴板共享数据做关联指纹
 *
 * 行为：
 *   - disableClipboardRead=true   →  getPrimaryClip() 一律返回 null (无内容)
 *   - fakeClipboardText!=null     →  返回一段用户自定义的文本
 * 同时覆盖：
 *   - ClipboardManager.getPrimaryClip()  / getPrimaryClipDescription()
 *   - getText() (deprecated 旧接口)
 *   - hasPrimaryClip()  → 若清空了也返回 false
 */
internal class ClipboardHooker(private val profile: HookProfiles) : YukiBaseHooker() {

    override fun onHook() {
        val cls = runCatching { "android.content.ClipboardManager".toClass() }.getOrNull() ?: return

        // 1. 返回空或伪造ClipData
        listOf("getPrimaryClip").forEach { methodName ->
            cls.method { name(methodName) }.hookAll().after {
                when {
                    profile.fakeClipboardText != null -> {
                        result = ClipData.newPlainText("guise_text", profile.fakeClipboardText)
                    }
                    profile.disableClipboardRead -> result = null
                }
            }
        }

        // 2. ClipDescription
        listOf("getPrimaryClipDescription").forEach { methodName ->
            cls.method { name(methodName) }.ignored().hookAll().after {
                when {
                    profile.fakeClipboardText != null -> {
                        result = ClipDescription("guise_text", arrayOf("text/plain"))
                    }
                    profile.disableClipboardRead -> result = null
                }
            }
        }

        // 3. hasPrimaryClip
        cls.method { name("hasPrimaryClip") }.hookAll().after {
            when {
                profile.fakeClipboardText != null -> result = true
                profile.disableClipboardRead -> result = false
            }
        }

        // 4. getText (deprecated 旧版)
        cls.method {
            name("getText")
        }.ignored().hookAll().after {
            when {
                profile.fakeClipboardText != null -> result = profile.fakeClipboardText
                profile.disableClipboardRead -> result = null
            }
        }

        // 5. hasText
        cls.method { name("hasText") }.ignored().hookAll().after {
            when {
                profile.fakeClipboardText != null -> result = true
                profile.disableClipboardRead -> result = false
            }
        }
    }
}
