package com.nanan.coc.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanan.coc.data.model.TechItem
import com.nanan.coc.data.repository.PlayerRepository
import com.nanan.coc.ui.viewmodel.TechUiState

private val CocAccent = Color(0xFF909090)
private val CocAccentLight = Color(0xFFE9E9E9)
private val CocTextPrimary = Color(0xFF404040)
private val CocTextSecondary = Color(0xFF909090)
private val CocBg = Color(0xFFFFFFFF)
private val CocGreen = Color(0xFF4CAF50)
private val CocOrange = Color(0xFFFF9800)
private val CocRed = Color(0xFFEF5350)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechScreen(
    uiState: TechUiState,
    onBack: () -> Unit,
    onLoadPlayer: (String, String) -> Unit,
    onToggleSettings: () -> Unit,
    onDismissSettings: () -> Unit
) {
    var tagInput by remember(uiState.playerTag) { mutableStateOf(uiState.playerTag) }
    var keyInput by remember(uiState.apiKey) { mutableStateOf(uiState.apiKey) }

    if (uiState.showSettings) {
        AlertDialog(
            onDismissRequest = onDismissSettings,
            title = { Text("API 设置", fontWeight = FontWeight.Medium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        label = { Text("玩家标签") },
                        placeholder = { Text("#ABC123") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("API Key") },
                        placeholder = { Text("在 developer.clashofclans.com 获取") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "API Key 请前往 developer.clashofclans.com 创建",
                        style = MaterialTheme.typography.bodySmall,
                        color = CocTextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { onLoadPlayer(tagInput, keyInput) }) {
                    Text("查询", color = CocAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissSettings) {
                    Text("取消", color = CocTextSecondary)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("科技查询", fontWeight = FontWeight.Bold, color = CocTextPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = CocTextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onToggleSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置", tint = CocTextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CocBg,
                    titleContentColor = CocTextPrimary
                ),
                windowInsets = WindowInsets.statusBars
            )
        },
        containerColor = CocBg,
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 快速输入栏
            QuickInputBar(
                tag = tagInput,
                apiKey = keyInput,
                onTagChange = { tagInput = it },
                onSearch = { onLoadPlayer(tagInput, keyInput) }
            )

            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CocAccent)
                    }
                }
                uiState.error != null && uiState.playerInfo == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(uiState.error!!, color = CocTextPrimary, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { onLoadPlayer(tagInput, keyInput) },
                                colors = ButtonDefaults.buttonColors(containerColor = CocAccent)
                            ) { Text("重试") }
                        }
                    }
                }
                uiState.playerInfo != null -> {
                    val allItems = uiState.techItems
                    val heroes = allItems.filter { it.category == "hero" }
                    val troops = allItems.filter { it.category == "troop" }
                    val spells = allItems.filter { it.category == "spell" }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 玩家信息头（精简）
                        PlayerHeaderCompact(uiState)

                        // 整体查询结果卡片
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 英雄等级
                                if (heroes.isNotEmpty()) {
                                    HeroSection(heroes)
                                }

                                // 兵种等级
                                if (troops.isNotEmpty()) {
                                    TroopSection(troops)
                                }

                                // 法术等级
                                if (spells.isNotEmpty()) {
                                    SpellSection(spells)
                                }
                            }
                        }
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = CocAccentLight,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "输入玩家标签和 API Key 查询科技发展",
                                color = CocTextSecondary,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "点击右上角 ⚙️ 设置",
                                color = CocAccentLight,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickInputBar(
    tag: String,
    apiKey: String,
    onTagChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    Surface(
        color = CocBg,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = tag,
                onValueChange = onTagChange,
                placeholder = { Text("玩家标签 #ABC123", color = Color(0xFFBDBDBD), fontSize = 13.sp) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CocAccent,
                    unfocusedBorderColor = CocAccentLight,
                    cursorColor = CocAccent
                ),
                modifier = Modifier.weight(1f).height(48.dp),
                textStyle = MaterialTheme.typography.bodyMedium
            )
            Surface(
                onClick = onSearch,
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(10.dp),
                color = CocAccent
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "查询",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerHeaderCompact(uiState: TechUiState) {
    val player = uiState.playerInfo ?: return

    Surface(
        color = Color(0xFFF5F5F5),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = CocAccent,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "TH${player.townHallLevel}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    player.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = CocTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    player.tag,
                    fontSize = 11.sp,
                    color = CocTextSecondary
                )
            }
            Text(
                "🏆 ${player.trophies}",
                fontSize = 12.sp,
                color = CocTextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── 英雄区域：只显示等级，不显示装备 ──

@Composable
private fun HeroSection(heroes: List<TechItem>) {
    SectionHeader(icon = "👑", title = "英雄等级")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        heroes.forEach { hero ->
            HeroChip(hero, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeroChip(item: TechItem, modifier: Modifier = Modifier) {
    val color = when {
        item.isMaxed -> CocGreen
        item.remainingLevels <= 2 -> CocOrange
        else -> CocRed
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                item.displayName,
                fontSize = 11.sp,
                color = CocTextPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "Lv.${item.currentLevel}/${item.boostMaxLevel}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

// ── 兵种区域 ──

@Composable
private fun TroopSection(troops: List<TechItem>) {
    val maxedCount = troops.count { it.isMaxed }
    val notMaxedCount = troops.size - maxedCount
    val totalRemainingTime = troops.filter { !it.isMaxed }.sumOf { it.totalUpgradeTime }

    SectionHeader(icon = "⚔️", title = "兵种等级")

    // 等级网格
    FlowGrid(items = troops, columns = 4)

    // 统计行
    Spacer(modifier = Modifier.height(4.dp))
    StatRow(maxedCount, notMaxedCount, totalRemainingTime, troops.size)
}

// ── 法术区域 ──

@Composable
private fun SpellSection(spells: List<TechItem>) {
    val maxedCount = spells.count { it.isMaxed }
    val notMaxedCount = spells.size - maxedCount
    val totalRemainingTime = spells.filter { !it.isMaxed }.sumOf { it.totalUpgradeTime }

    SectionHeader(icon = "✨", title = "法术等级")

    // 等级网格
    FlowGrid(items = spells, columns = 4)

    // 统计行
    Spacer(modifier = Modifier.height(4.dp))
    StatRow(maxedCount, notMaxedCount, totalRemainingTime, spells.size)
}

// ── 通用组件 ──

@Composable
private fun SectionHeader(icon: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = CocTextPrimary
        )
    }
}

@Composable
private fun FlowGrid(items: List<TechItem>, columns: Int) {
    val rows = items.chunked(columns)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                rowItems.forEach { item ->
                    LevelChip(item, modifier = Modifier.weight(1f))
                }
                // 填充空位
                repeat(columns - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LevelChip(item: TechItem, modifier: Modifier = Modifier) {
    val color = when {
        item.isMaxed -> CocGreen
        item.remainingLevels <= 2 -> CocOrange
        else -> CocRed
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.1f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                item.displayName,
                fontSize = 10.sp,
                color = CocTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "Lv.${item.currentLevel}/${item.boostMaxLevel}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun StatRow(maxedCount: Int, notMaxedCount: Int, totalRemainingTime: Long, total: Int) {
    val percent = if (total > 0) (maxedCount * 100 / total) else 0

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFF0F0F0),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "✅ $maxedCount 已满  ❌ $notMaxedCount 未满  ($percent%)",
                fontSize = 11.sp,
                color = CocTextPrimary
            )
            if (totalRemainingTime > 0) {
                Text(
                    "⏱ ${PlayerRepository.formatTime(totalRemainingTime)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = CocOrange
                )
            } else {
                Text(
                    "全部满级",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = CocGreen
                )
            }
        }
    }
}
