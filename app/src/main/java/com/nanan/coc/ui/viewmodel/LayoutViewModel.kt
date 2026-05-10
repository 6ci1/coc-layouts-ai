package com.nanan.coc.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanan.coc.auth.AuthApi
import com.nanan.coc.data.model.LayoutItem
import com.nanan.coc.data.repository.LayoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class SortMode(val label: String) {
    DEFAULT("默认"),
    TH_DESC("TH等级↓"),
    TH_ASC("TH等级↑")
}

data class LayoutUiState(
    val layouts: List<LayoutItem> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val lastUpdateMessage: String? = null,
    val notice: String? = null,
    val selectedServer: String? = null,
    val selectedThLevel: Int? = null,
    val cnThLevels: List<Int> = emptyList(),
    val enThLevels: List<Int> = emptyList(),
    val cnDocId: String = "",
    val enDocId: String = "",
    val sortMode: SortMode = SortMode.DEFAULT,
    val favoriteLinks: Set<String> = emptySet(),
    val showFavoritesOnly: Boolean = false,
    val linkHistory: List<Triple<String, Long, String>> = emptyList(),  // (link, time, "server:thLevel")
)

@HiltViewModel
class LayoutViewModel @Inject constructor(
    private val repository: LayoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LayoutUiState())
    val uiState: StateFlow<LayoutUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

    init {
        loadLayouts()
        loadLinkHistory()
    }

    fun loadLayouts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // 拉取网络公告
            val notice = AuthApi.getNotice()

            val cnDoc = repository.getCnDocId()
            val enDoc = repository.getEnDocId()
            val favLinks = repository.getFavoriteLinks()

            val cached = repository.getCachedLayouts()
            if (cached.isNotEmpty()) {
                val lastUpdate = repository.getLastUpdateTime()
                val msg = if (lastUpdate > 0L) "上次更新: ${dateFormat.format(Date(lastUpdate))}" else null
                val levels = buildThLevels(cached)
                _uiState.value = _uiState.value.copy(
                    layouts = cached,
                    isLoading = false,
                    lastUpdateMessage = msg,
                    notice = notice,
                    cnThLevels = levels["cn"] ?: emptyList(),
                    enThLevels = levels["en"] ?: emptyList(),
                    cnDocId = cnDoc,
                    enDocId = enDoc,
                    favoriteLinks = favLinks
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    cnDocId = cnDoc, enDocId = enDoc,
                    favoriteLinks = favLinks,
                    notice = notice
                )
            }

            checkForUpdates()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            checkForUpdates()
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    fun selectServer(server: String?) {
        _uiState.value = _uiState.value.copy(selectedServer = server, selectedThLevel = null)
    }

    fun selectThLevel(level: Int?) {
        _uiState.value = _uiState.value.copy(selectedThLevel = level)
    }

    fun updateDocIds(cnDoc: String, enDoc: String) {
        viewModelScope.launch {
            repository.setDocIds(cnDoc, enDoc)
            _uiState.value = _uiState.value.copy(cnDocId = cnDoc, enDocId = enDoc)
            _uiState.value = _uiState.value.copy(isLoading = true)
            checkForUpdates()
        }
    }

    fun setSortMode(mode: SortMode) {
        _uiState.value = _uiState.value.copy(sortMode = mode)
    }

    fun toggleFavorite(link: String, imageUrl: String) {
        viewModelScope.launch {
            val isFav = repository.toggleFavorite(link, imageUrl)
            val current = _uiState.value.favoriteLinks.toMutableSet()
            if (isFav) current.add(link) else current.remove(link)
            _uiState.value = _uiState.value.copy(favoriteLinks = current)
        }
    }

    fun toggleShowFavoritesOnly() {
        _uiState.value = _uiState.value.copy(showFavoritesOnly = !_uiState.value.showFavoritesOnly)
    }

    fun isFavorite(link: String): Boolean {
        return link in _uiState.value.favoriteLinks
    }

    fun addLinkHistory(link: String, server: String = "", thLevel: Int = 0) {
        viewModelScope.launch {
            val prefs = repository.getPrefs()
            val timestamp = System.currentTimeMillis()
            val hashCode = link.hashCode()
            // Store the actual link
            prefs.edit().putString("link_$hashCode", link).apply()
            // Store the server:thLevel
            val svrData = if (server.isNotBlank()) {
                if (thLevel > 0) "$server:$thLevel" else server
            } else ""
            if (svrData.isNotBlank()) {
                prefs.edit().putString("link_svr_$hashCode", svrData).apply()
            }
            // Add to history set
            val history = prefs.getStringSet("link_history", emptySet())?.toMutableSet() ?: mutableSetOf()
            // Remove existing entry for this link
            history.removeAll { it.endsWith("|$hashCode") }
            history.add("$timestamp|$hashCode")
            // Keep max 50 entries
            val trimmed = history.sortedByDescending { it.split("|")[0].toLongOrNull() ?: 0 }.take(50).toSet()
            prefs.edit().putStringSet("link_history", trimmed).apply()
            loadLinkHistory()
        }
    }

    fun removeLinkHistory(link: String) {
        viewModelScope.launch {
            val prefs = repository.getPrefs()
            val hashCode = link.hashCode()
            // Remove from history set
            val history = prefs.getStringSet("link_history", emptySet())?.toMutableSet() ?: mutableSetOf()
            history.removeAll { it.endsWith("|$hashCode") }
            prefs.edit().putStringSet("link_history", history).apply()
            // Clean up stored data
            prefs.edit()
                .remove("link_$hashCode")
                .remove("link_svr_$hashCode")
                .apply()
            loadLinkHistory()
        }
    }

    private fun loadLinkHistory() {
        viewModelScope.launch {
            val prefs = repository.getPrefs()
            val historySet = prefs.getStringSet("link_history", emptySet()) ?: emptySet()
            val history = historySet.mapNotNull { entry ->
                val parts = entry.split("|")
                if (parts.size >= 2) {
                    val time = parts[0].toLongOrNull() ?: return@mapNotNull null
                    val hashCode = parts[1].toIntOrNull() ?: return@mapNotNull null
                    val link = prefs.getString("link_$hashCode", null) ?: return@mapNotNull null
                    val server = prefs.getString("link_svr_$hashCode", "") ?: ""
                    Triple(link, time, server)
                } else null
            }.sortedByDescending { it.second }
            _uiState.value = _uiState.value.copy(linkHistory = history)
        }
    }

    private fun buildThLevels(layouts: List<LayoutItem>): Map<String, List<Int>> {
        val cn = layouts.filter { it.server == "cn" }.map { it.thLevel }.filter { it > 0 }.distinct().sortedDescending()
        val en = layouts.filter { it.server == "en" }.map { it.thLevel }.filter { it > 0 }.distinct().sortedDescending()
        return mapOf("cn" to cn, "en" to en)
    }

    private suspend fun checkForUpdates() {
        val result = repository.checkAndUpdate()
        result.fold(
            onSuccess = { updated ->
                if (updated || _uiState.value.layouts.isEmpty()) {
                    updateStateWithFreshData()
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            },
            onFailure = { e ->
                if (_uiState.value.layouts.isEmpty()) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "加载失败")
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, lastUpdateMessage = "更新失败，使用缓存")
                }
            }
        )
    }

    private suspend fun updateStateWithFreshData() {
        val notice = AuthApi.getNotice()
        val fresh = repository.getCachedLayouts()
        val lastUpdate = repository.getLastUpdateTime()
        val msg = if (lastUpdate > 0L) "上次更新: ${dateFormat.format(Date(lastUpdate))}" else "已更新"
        val levels = buildThLevels(fresh)
        _uiState.value = _uiState.value.copy(
            layouts = fresh,
            isLoading = false,
            lastUpdateMessage = msg,
            notice = notice,
            cnThLevels = levels["cn"] ?: emptyList(),
            enThLevels = levels["en"] ?: emptyList(),
            selectedThLevel = _uiState.value.selectedThLevel?.takeIf { th ->
                when (_uiState.value.selectedServer) {
                    "cn" -> th in (levels["cn"] ?: emptyList())
                    "en" -> th in (levels["en"] ?: emptyList())
                    else -> th in fresh.map { it.thLevel }
                }
            }
        )
    }
}
