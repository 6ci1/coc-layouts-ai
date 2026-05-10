package com.nanan.coc.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {

    sealed class AuthState {
        /** 应用启动，检查中 */
        data object Checking : AuthState()
        /** 已激活，进入主界面 */
        data object Activated : AuthState()
        /** 需要登录 */
        data object NeedLogin : AuthState()
        /** 登录中 */
        data object LoggingIn : AuthState()
        /** 游客模式（无卡密） */
        data object Guest : AuthState()
    }

    sealed class UiEvent {
        data class Toast(val message: String) : UiEvent()
        data class ShowNotice(val message: String) : UiEvent()
    }

    private val _state = MutableStateFlow<AuthState>(AuthState.Checking)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _events = MutableStateFlow<UiEvent?>(null)
    val events: StateFlow<UiEvent?> = _events.asStateFlow()

    var notice: String? = null
        private set

    fun checkAuth() {
        viewModelScope.launch {
            if (authManager.hasValidLocalAuth()) {
                // 本地凭据有效，仍需向服务器做心跳验证
                val kami = authManager.getKami() ?: ""
                val token = authManager.getToken() ?: ""
                val result = AuthApi.heartbeat(kami, authManager.deviceId, token)
                if (result.valid) {
                    // 更新本地到期时间（服务器可能已延长期限）
                    if (result.expiry != null && result.expiry > 0) {
                        authManager.saveAuth(
                            kami = kami,
                            token = token,
                            expiry = result.expiry,
                            kmtype = authManager.getKmType()
                        )
                    }
                    _state.value = AuthState.Activated
                } else {
                    // 服务器验证失败（卡密已删除/过期），清除本地凭据
                    authManager.clearAuth()
                    val n = AuthApi.getNotice()
                    if (!n.isNullOrBlank()) {
                        notice = n
                        _events.value = UiEvent.ShowNotice(n)
                    }
                    _events.value = UiEvent.Toast("卡密已失效：${result.message}")
                    _state.value = AuthState.NeedLogin
                }
            } else {
                // 同时拉取公告
                val n = AuthApi.getNotice()
                if (!n.isNullOrBlank()) {
                    notice = n
                    _events.value = UiEvent.ShowNotice(n)
                }
                _state.value = AuthState.NeedLogin
            }
        }
    }

    /**
     * 运行时定期心跳，验证卡密是否仍有效
     * 如果失效，清除凭据并切换到登录界面
     */
    fun checkHeartbeat() {
        if (_state.value != AuthState.Activated) return
        viewModelScope.launch {
            val kami = authManager.getKami() ?: return@launch
            val token = authManager.getToken() ?: ""
            val result = AuthApi.heartbeat(kami, authManager.deviceId, token)
            if (!result.valid) {
                authManager.clearAuth()
                _events.value = UiEvent.Toast("卡密已失效，请重新激活")
                _state.value = AuthState.NeedLogin
            } else if (result.expiry != null && result.expiry > 0) {
                authManager.saveAuth(
                    kami = kami,
                    token = token,
                    expiry = result.expiry,
                    kmtype = authManager.getKmType()
                )
            }
        }
    }

    fun login(kami: String) {
        if (kami.isBlank()) {
            _events.value = UiEvent.Toast("请输入卡密")
            return
        }
        viewModelScope.launch {
            _state.value = AuthState.LoggingIn
            val result = AuthApi.login(kami, authManager.deviceId)
            if (result.success) {
                authManager.saveAuth(
                    kami = kami,
                    token = result.token ?: "",
                    expiry = result.expiry ?: 0,
                    kmtype = result.kmtype
                )
                val expireMsg = if (result.expiry != null && result.expiry > 0) {
                    val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                        .format(java.util.Date(result.expiry * 1000))
                    "，到期时间：$date"
                } else ""
                _events.value = UiEvent.Toast("${result.message}$expireMsg")
                _state.value = AuthState.Activated
            } else {
                _events.value = UiEvent.Toast(result.message)
                _state.value = AuthState.NeedLogin
            }
        }
    }

    fun unbind(kami: String) {
        viewModelScope.launch {
            val result = AuthApi.unbind(kami, authManager.deviceId)
            if (result.success) {
                authManager.clearAuth()
                _events.value = UiEvent.Toast("解绑成功，剩余解绑次数：${result.remainingCount ?: "未知"}")
                _state.value = AuthState.NeedLogin
            } else {
                _events.value = UiEvent.Toast(result.message)
            }
        }
    }

    fun clearEvent() {
        _events.value = null
    }

    fun enterGuest() {
        _state.value = AuthState.Guest
    }

    fun exitGuest() {
        authManager.clearAuth()
        _state.value = AuthState.NeedLogin
    }
}
