package com.nanan.coc.ui.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanan.coc.data.model.PlayerInfo
import com.nanan.coc.data.model.TechItem
import com.nanan.coc.data.repository.PlayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TechUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val playerInfo: PlayerInfo? = null,
    val techItems: List<TechItem> = emptyList(),
    val playerTag: String = "",
    val apiKey: String = "",
    val selectedCategory: TechCategory = TechCategory.ALL,
    val showSettings: Boolean = false,
    val totalRemainingTime: Long = 0L,
    val maxedCount: Int = 0,
    val totalCount: Int = 0
)

enum class TechCategory(val label: String) {
    ALL("全部"), TROOP("兵种"), SPELL("法术"), HERO("英雄")
}

@HiltViewModel
class TechViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playerRepository: PlayerRepository
) : ViewModel() {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("tech_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(TechUiState())
    val uiState: StateFlow<TechUiState> = _uiState.asStateFlow()

    init {
        val savedTag = prefs.getString("player_tag", "") ?: ""
        val savedKey = prefs.getString("api_key", "") ?: ""
        _uiState.value = _uiState.value.copy(
            playerTag = savedTag,
            apiKey = savedKey
        )
        // 如果有保存的 tag 和 key，自动加载
        if (savedTag.isNotBlank() && savedKey.isNotBlank()) {
            loadPlayer(savedTag, savedKey)
        }
    }

    fun loadPlayer(tag: String, key: String) {
        val trimmedTag = tag.trim()
        val trimmedKey = key.trim()
        if (trimmedTag.isBlank() || trimmedKey.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "请输入玩家标签和API Key")
            return
        }

        // 保存
        prefs.edit()
            .putString("player_tag", trimmedTag)
            .putString("api_key", trimmedKey)
            .apply()

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null,
            playerTag = trimmedTag,
            apiKey = trimmedKey
        )

        viewModelScope.launch {
            val result = playerRepository.getPlayer(trimmedTag, trimmedKey)
            result.fold(
                onSuccess = { player ->
                    val techItems = playerRepository.calculateTech(player)
                    val totalTime = techItems.sumOf { it.totalUpgradeTime }
                    val maxedCount = techItems.count { it.isMaxed }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        playerInfo = player,
                        techItems = techItems,
                        totalRemainingTime = totalTime,
                        maxedCount = maxedCount,
                        totalCount = techItems.size,
                        showSettings = false
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "未知错误"
                    )
                }
            )
        }
    }

    fun setCategory(cat: TechCategory) {
        _uiState.value = _uiState.value.copy(selectedCategory = cat)
    }

    fun toggleSettings() {
        _uiState.value = _uiState.value.copy(
            showSettings = !_uiState.value.showSettings
        )
    }

    fun dismissSettings() {
        _uiState.value = _uiState.value.copy(showSettings = false)
    }

    fun getFilteredItems(): List<TechItem> {
        val state = _uiState.value
        return when (state.selectedCategory) {
            TechCategory.ALL -> state.techItems
            TechCategory.TROOP -> state.techItems.filter { it.category == "troop" }
            TechCategory.SPELL -> state.techItems.filter { it.category == "spell" }
            TechCategory.HERO -> state.techItems.filter { it.category == "hero" }
        }.sortedWith(
            compareByDescending<TechItem> { it.remainingLevels > 0 }
                .thenByDescending { it.totalUpgradeTime }
        )
    }
}
