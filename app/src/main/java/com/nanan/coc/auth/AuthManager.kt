package com.nanan.coc.auth

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 验证状态管理
 */
@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("coc_auth", Context.MODE_PRIVATE)

    /** 设备唯一标识 (ANDROID_ID 的 SHA-256) */
    val deviceId: String by lazy {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"
        CryptoUtil.sha256(androidId)
    }

    var isActivated: Boolean
        get() = getToken() != null && getExpiry() > System.currentTimeMillis() / 1000
        private set(_) {}

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    fun getKami(): String? = prefs.getString(KEY_KAMI, null)
    fun getExpiry(): Long = prefs.getLong(KEY_EXPIRY, 0)
    fun getKmType(): String? = prefs.getString(KEY_KMTYPE, null)

    fun saveAuth(kami: String, token: String, expiry: Long, kmtype: String?) {
        prefs.edit()
            .putString(KEY_KAMI, kami)
            .putString(KEY_TOKEN, token)
            .putLong(KEY_EXPIRY, expiry)
            .putString(KEY_KMTYPE, kmtype)
            .apply()
    }

    fun clearAuth() {
        prefs.edit()
            .remove(KEY_KAMI)
            .remove(KEY_TOKEN)
            .remove(KEY_EXPIRY)
            .remove(KEY_KMTYPE)
            .apply()
    }

    /**
     * 检查本地是否有未过期的凭据
     */
    fun hasValidLocalAuth(): Boolean {
        val kami = getKami() ?: return false
        val token = getToken()
        // 有卡密即视为已激活（免费模式无 token，付费模式 token 可能为空字符串）
        if (token.isNullOrEmpty()) return true
        val expiry = getExpiry()
        return expiry == 0L || expiry > System.currentTimeMillis() / 1000
    }

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_KAMI = "auth_kami"
        private const val KEY_EXPIRY = "auth_expiry"
        private const val KEY_KMTYPE = "auth_kmtype"
    }
}
