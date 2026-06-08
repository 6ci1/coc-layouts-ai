package com.nanan.coc.ui.screen

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.nanan.coc.R
import com.nanan.coc.data.model.LayoutItem
import com.nanan.coc.ui.components.LayoutGridShimmer
import com.nanan.coc.ui.viewmodel.LayoutUiState
import com.nanan.coc.ui.viewmodel.SortMode
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

// 配色 - 纯白低灰
private val CocAccent = Color(0xFF909090)
private val CocAccentLight = Color(0xFFE9E9E9)
private val CocAccentDark = Color(0xFF505050)
private val CocBg = Color(0xFFFFFFFF)
private val CocCard = Color.White
private val CocTextPrimary = Color(0xFF404040)
private val CocTextSecondary = Color(0xFF909090)
private val CocTextHint = Color(0xFFE9E9E9)
private val CocFavorite = Color(0xFFEF5350)

/**
 * 带按压缩放效果的按钮
 */
@Composable
private fun ScaleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "btnScale"
    )

    Surface(
        onClick = onClick,
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(10.dp),
        color = color,
        enabled = enabled,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutGridScreen(
    uiState: LayoutUiState,
    onRefresh: () -> Unit,
    onLayoutClick: (LayoutItem) -> Unit,
    onSelectServer: (String?) -> Unit,
    onSelectThLevel: (Int?) -> Unit,
    onUpdateDocIds: (String, String) -> Unit,
    onSortModeChange: (SortMode) -> Unit,
    onToggleFavorite: (String, String) -> Unit,
    onToggleFavoritesOnly: () -> Unit,
    isFavorite: (String) -> Boolean,
    onShare: (String) -> Unit,
    onOpenCustomLink: (String, String) -> Unit = { _, _ -> },
    onAddLinkHistory: (String, String, Int) -> Unit = { _, _, _ -> },
    onRemoveLinkHistory: (String) -> Unit = {}
) {
    var selectedItem by remember { mutableStateOf<LayoutItem?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var showCustomLink by remember { mutableStateOf(false) }
    var titleClickCount by remember { mutableStateOf(0) }
    var lastClickTime by remember { mutableStateOf(0L) }
    var showSortMenu by remember { mutableStateOf(false) }

    val filteredLayouts = remember(
        uiState.layouts, uiState.selectedServer, uiState.selectedThLevel,
        uiState.sortMode, uiState.showFavoritesOnly, uiState.favoriteLinks
    ) {
        uiState.layouts.filter { item ->
            val serverMatch = uiState.selectedServer == null || item.server == uiState.selectedServer
            val thMatch = uiState.selectedThLevel == null || item.thLevel == uiState.selectedThLevel
            val favMatch = !uiState.showFavoritesOnly || item.link in uiState.favoriteLinks
            serverMatch && thMatch && favMatch
        }.let { list ->
            when (uiState.sortMode) {
                SortMode.TH_DESC -> list.sortedByDescending { it.thLevel }
                SortMode.TH_ASC -> list.sortedBy { it.thLevel }
                SortMode.DEFAULT -> list
            }
        }
    }

    val currentThLevels = when (uiState.selectedServer) {
        "cn" -> uiState.cnThLevels
        "en" -> uiState.enThLevels
        else -> (uiState.cnThLevels + uiState.enThLevels).distinct().sortedDescending()
    }

    val onTitleClick: () -> Unit = {
        val now = System.currentTimeMillis()
        if (now - lastClickTime > 1500) titleClickCount = 0
        lastClickTime = now
        titleClickCount++
        if (titleClickCount >= 6) {
            titleClickCount = 0
            showSettings = true
        }
    }

    if (showSettings) {
        SettingsDialog(
            cnDocId = uiState.cnDocId,
            enDocId = uiState.enDocId,
            onDismiss = { showSettings = false },
            onSave = { cnDoc, enDoc ->
                showSettings = false
                onUpdateDocIds(cnDoc, enDoc)
            }
        )
    }

    if (showLinkDialog) {
        OpenLinkDialog(
            onDismiss = { showLinkDialog = false },
            onOpen = { link, server ->
                showLinkDialog = false
                val thLevel = Regex("TH(\\d+)").find(link.trim())?.groupValues?.get(1)?.toIntOrNull() ?: 0
                onAddLinkHistory(link, server, thLevel)
                onOpenCustomLink(link, server)
            },
            history = uiState.linkHistory,
            onRemoveHistory = onRemoveLinkHistory
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "COC阵型",
                        fontWeight = FontWeight.Bold,
                        color = CocTextPrimary,
                        modifier = Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onTitleClick() }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CocBg,
                    titleContentColor = CocTextPrimary
                ),
                windowInsets = WindowInsets.statusBars
            )
        },
        bottomBar = {
            Surface(
                color = CocBg,
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 云更新按钮 - 带旋转动画
                    val refreshInteraction = remember { MutableInteractionSource() }
                    val isRefreshPressed by refreshInteraction.collectIsPressedAsState()
                    val refreshScale by animateFloatAsState(
                        targetValue = if (isRefreshPressed) 0.93f else 1f,
                        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                        label = "refreshScale"
                    )

                    Surface(
                        onClick = onRefresh,
                        modifier = Modifier.weight(1f).height(42.dp).scale(refreshScale),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFE0E0E0),
                        interactionSource = refreshInteraction
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 刷新时图标旋转
                            val rotation by animateFloatAsState(
                                targetValue = if (uiState.isRefreshing) 360f else 0f,
                                animationSpec = if (uiState.isRefreshing) {
                                    infiniteRepeatable(
                                        animation = tween(1000, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                    )
                                } else { tween(300) },
                                label = "refreshRotation"
                            )
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "云更新",
                                tint = Color(0xFF616161),
                                modifier = Modifier
                                    .size(18.dp)
                                    .graphicsLayer { rotationZ = rotation }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("云更新", fontSize = 13.sp, color = Color(0xFF616161), fontWeight = FontWeight.Medium)
                        }
                    }

                    // 自定义链接按钮
                    val linkInteraction = remember { MutableInteractionSource() }
                    val isLinkPressed by linkInteraction.collectIsPressedAsState()
                    val linkScale by animateFloatAsState(
                        targetValue = if (isLinkPressed) 0.93f else 1f,
                        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                        label = "linkScale"
                    )

                    Surface(
                        onClick = { showLinkDialog = true },
                        modifier = Modifier.weight(1f).height(42.dp).scale(linkScale),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFE0E0E0),
                        interactionSource = linkInteraction
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "自定义链接",
                                tint = Color(0xFF616161),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("自定义链接", fontSize = 13.sp, color = Color(0xFF616161), fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        },
        containerColor = CocBg,
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // 使用 crossfade 在加载/错误/内容之间平滑切换
            Crossfade(
                targetState = when {
                    uiState.isLoading && uiState.layouts.isEmpty() -> "loading"
                    uiState.error != null && uiState.layouts.isEmpty() -> "error"
                    else -> "content"
                },
                animationSpec = tween(400),
                label = "contentCrossfade"
            ) { state ->
                when (state) {
                    "loading" -> {
                        // 骨架屏 shimmer 加载
                        LayoutGridShimmer()
                    }
                    "error" -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(uiState.error ?: "", color = CocTextPrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onRefresh,
                                colors = ButtonDefaults.buttonColors(containerColor = CocAccent)
                            ) { Text("重试") }
                        }
                    }
                    "content" -> {
                        SwipeRefresh(
                            state = rememberSwipeRefreshState(uiState.isRefreshing),
                            onRefresh = onRefresh
                        ) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 公告 + 排序 + 收藏
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    AnimatedVisibility(
                                        visible = true,
                                        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { -20 }
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            uiState.notice?.let { msg ->
                                                Text(
                                                    text = msg,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = CocAccentDark,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            } ?: Spacer(modifier = Modifier.weight(1f))

                                            // 收藏筛选 - 带心跳动画
                                            val favScale by animateFloatAsState(
                                                targetValue = if (uiState.showFavoritesOnly) 1.15f else 1f,
                                                animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
                                                label = "favScale"
                                            )
                                            IconButton(onClick = onToggleFavoritesOnly, modifier = Modifier.size(32.dp)) {
                                                Icon(
                                                    if (uiState.showFavoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                    contentDescription = "收藏",
                                                    tint = if (uiState.showFavoritesOnly) CocFavorite else CocTextHint,
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .scale(favScale)
                                                )
                                            }

                                            // 排序
                                            Box {
                                                IconButton(onClick = { showSortMenu = true }, modifier = Modifier.size(32.dp)) {
                                                    Icon(Icons.Default.Sort, contentDescription = "排序", tint = CocTextSecondary, modifier = Modifier.size(18.dp))
                                                }
                                                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                                    SortMode.entries.forEach { mode ->
                                                        DropdownMenuItem(
                                                            text = {
                                                                Text(
                                                                    mode.label,
                                                                    fontSize = 13.sp,
                                                                    color = if (mode == uiState.sortMode) CocAccent else CocTextPrimary
                                                                )
                                                            },
                                                            onClick = {
                                                                onSortModeChange(mode)
                                                                showSortMenu = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // 服务器分类
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    AnimatedVisibility(
                                        visible = true,
                                        enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { -15 }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(CocCard)
                                                .horizontalScroll(rememberScrollState())
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            AnimatedChip("全部", uiState.selectedServer == null) { onSelectServer(null) }
                                            if (uiState.cnThLevels.isNotEmpty()) {
                                                AnimatedChip("国服", uiState.selectedServer == "cn") { onSelectServer("cn") }
                                            }
                                            if (uiState.enThLevels.isNotEmpty()) {
                                                AnimatedChip("国际服", uiState.selectedServer == "en") { onSelectServer("en") }
                                            }
                                        }
                                    }
                                }

                                // TH等级分类
                                if (currentThLevels.isNotEmpty()) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        AnimatedVisibility(
                                            visible = true,
                                            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { -15 }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(CocCard)
                                                    .horizontalScroll(rememberScrollState())
                                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                AnimatedChipSmall("全部", uiState.selectedThLevel == null) { onSelectThLevel(null) }
                                                currentThLevels.forEach { level ->
                                                    AnimatedChipSmall("TH$level", uiState.selectedThLevel == level) { onSelectThLevel(level) }
                                                }
                                            }
                                        }
                                    }
                                }

                                // 数量统计
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    // 数量变化时的动画
                                    val count = filteredLayouts.size
                                    val animatedCount by animateIntAsState(
                                        targetValue = count,
                                        animationSpec = tween(300),
                                        label = "countAnim"
                                    )
                                    Text(
                                        text = "共 ${animatedCount} 个阵型",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CocTextSecondary
                                    )
                                }

                                // 阵型卡片 - 带入场动画
                                items(
                                    filteredLayouts,
                                    key = { it.link }
                                ) { item ->
                                    // 每张卡片渐现 + 上滑入场
                                    var visible by remember { mutableStateOf(false) }
                                    LaunchedEffect(item.link) {
                                        visible = true
                                    }
                                    AnimatedVisibility(
                                        visible = visible,
                                        enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { 40 }
                                    ) {
                                        LayoutCard(
                                            item = item,
                                            isFavorite = isFavorite(item.link),
                                            onClick = { selectedItem = item },
                                            onUse = { onLayoutClick(it) },
                                            onToggleFavorite = { onToggleFavorite(item.link, item.imageUrl) },
                                            onShare = { onShare(item.link) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedItem?.let { item ->
        LayoutPreviewDialog(
            item = item,
            isFavorite = isFavorite(item.link),
            onToggleFavorite = { onToggleFavorite(item.link, item.imageUrl) },
            onShare = { onShare(item.link) },
            onDismiss = { selectedItem = null }
        )
    }
}

/**
 * 带选中动画的标签
 */
@Composable
fun AnimatedChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) CocAccent else CocAccentLight.copy(alpha = 0.4f),
        animationSpec = tween(250),
        label = "chipBg"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "chipScale"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        modifier = Modifier
            .height(32.dp)
            .scale(scale)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
            Text(
                text = text,
                fontSize = 13.sp,
                color = if (selected) Color.White else CocAccentDark,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

/**
 * 带选中动画的小标签
 */
@Composable
fun AnimatedChipSmall(text: String, selected: Boolean, onClick: () -> Unit) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) CocAccent else CocAccentLight,
        animationSpec = tween(250),
        label = "chipSmallBg"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "chipSmallScale"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        modifier = Modifier
            .height(28.dp)
            .scale(scale)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
            Text(
                text = text,
                fontSize = 12.sp,
                color = if (selected) Color.White else CocTextSecondary
            )
        }
    }
}

@Composable
fun SettingsDialog(
    cnDocId: String,
    enDocId: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var cnDoc by remember { mutableStateOf(cnDocId) }
    var enDoc by remember { mutableStateOf(enDocId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置文档链接") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = cnDoc,
                    onValueChange = { cnDoc = it },
                    label = { Text("国服阵型文档ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = enDoc,
                    onValueChange = { enDoc = it },
                    label = { Text("国际服阵型文档ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "在腾讯文档URL中，/doc/ 后面的那串字符就是文档ID",
                    style = MaterialTheme.typography.bodySmall,
                    color = CocTextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(cnDoc.trim(), enDoc.trim()) }) {
                Text("保存并刷新", color = CocAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = CocTextSecondary)
            }
        }
    )
}

@Composable
fun OpenLinkDialog(
    onDismiss: () -> Unit,
    onOpen: (String, String) -> Unit,
    history: List<Triple<String, Long, String>> = emptyList(),
    onRemoveHistory: (String) -> Unit = {}
) {
    var link by remember { mutableStateOf("") }
    val hasLink = link.isNotBlank()

    val detectedServer = remember(link) {
        val l = link.lowercase()
        when {
            l.contains("/cn/") || l.contains("link.clashofclans.com/cn") -> "cn"
            l.contains("/en/") || l.contains("link.clashofclans.com/en") -> "en"
            else -> ""
        }
    }

    val lightGray = Color(0xFFE0E0E0)
    val darkGray = Color(0xFF757575)
    val btnCn by animateColorAsState(
        if (hasLink && (detectedServer == "cn" || detectedServer.isEmpty())) darkGray else lightGray,
        label = "cn", animationSpec = tween(300)
    )
    val btnEn by animateColorAsState(
        if (hasLink && (detectedServer == "en" || detectedServer.isEmpty())) darkGray else lightGray,
        label = "en", animationSpec = tween(300)
    )
    val textCn by animateColorAsState(
        if (hasLink && (detectedServer == "cn" || detectedServer.isEmpty())) Color.White else Color(0xFF9E9E9E),
        label = "cnT", animationSpec = tween(300)
    )
    val textEn by animateColorAsState(
        if (hasLink && (detectedServer == "en" || detectedServer.isEmpty())) Color.White else Color(0xFF9E9E9E),
        label = "enT", animationSpec = tween(300)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("输入阵型链接", fontWeight = FontWeight.Medium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = link,
                    onValueChange = { newValue ->
                        val prefix = "https://link.clashofclans.com/"
                        if (newValue.isEmpty()
                            || newValue.startsWith(prefix)
                            || prefix.startsWith(newValue)
                        ) {
                            link = newValue
                        }
                    },
                    placeholder = { Text("粘贴阵型链接...", color = Color(0xFFBDBDBD)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = darkGray,
                        unfocusedBorderColor = lightGray,
                        cursorColor = darkGray
                    ),
                    trailingIcon = {
                        AnimatedVisibility(
                            visible = link.isNotEmpty(),
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
                        ) {
                            IconButton(onClick = { link = "" }, modifier = Modifier.size(20.dp)) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "清除",
                                    tint = Color(0xFFBDBDBD),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 按钮带缩放动画
                    val cnInteraction = remember { MutableInteractionSource() }
                    val isCnPressed by cnInteraction.collectIsPressedAsState()
                    val cnScale by animateFloatAsState(
                        targetValue = if (isCnPressed) 0.93f else 1f,
                        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                        label = "cnBtnScale"
                    )

                    Surface(
                        onClick = { if (hasLink) onOpen(link.trim(), "cn") },
                        modifier = Modifier.weight(1f).height(44.dp).scale(cnScale),
                        shape = RoundedCornerShape(10.dp),
                        color = btnCn,
                        enabled = hasLink,
                        interactionSource = cnInteraction
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("国服打开", color = textCn, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                    }

                    val enInteraction = remember { MutableInteractionSource() }
                    val isEnPressed by enInteraction.collectIsPressedAsState()
                    val enScale by animateFloatAsState(
                        targetValue = if (isEnPressed) 0.93f else 1f,
                        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                        label = "enBtnScale"
                    )

                    Surface(
                        onClick = { if (hasLink) onOpen(link.trim(), "en") },
                        modifier = Modifier.weight(1f).height(44.dp).scale(enScale),
                        shape = RoundedCornerShape(10.dp),
                        color = btnEn,
                        enabled = hasLink,
                        interactionSource = enInteraction
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("国际服打开", color = textEn, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                    }
                }

                // History
                AnimatedVisibility(
                    visible = history.isNotEmpty(),
                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                    exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "历史记录",
                            fontSize = 12.sp,
                            color = Color(0xFF9E9E9E),
                            fontWeight = FontWeight.Medium
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            history.take(20).forEach { (hLink, hTime, hServer) ->
                                val dateStr = remember(hTime) {
                                    val sdf = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
                                    sdf.format(java.util.Date(hTime))
                                }
                                val svrParts = hServer.split(":")
                                val svr = svrParts.getOrElse(0) { "" }
                                val thLvl = svrParts.getOrElse(1) { "0" }.toIntOrNull() ?: 0
                                val (tagText, tagBg, tagFg) = when (svr) {
                                    "cn" -> Triple(
                                        if (thLvl > 0) "国服${thLvl}本" else "国服",
                                        Color(0xFFE8F5E9), Color(0xFF388E3C)
                                    )
                                    "en" -> Triple(
                                        if (thLvl > 0) "国际${thLvl}本" else "国际",
                                        Color(0xFFE3F2FD), Color(0xFF1976D2)
                                    )
                                    else -> Triple("", Color.Transparent, Color.Transparent)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().height(40.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { onRemoveHistory(hLink) },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "删除",
                                            tint = Color(0xFFBDBDBD),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        onClick = { link = hLink },
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF5F5F5)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = Color(0xFFBDBDBD),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            if (tagText.isNotEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(tagBg, RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                                ) {
                                                    Text(
                                                        tagText,
                                                        fontSize = 10.sp,
                                                        color = tagFg,
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(5.dp))
                                            }
                                            Text(
                                                hLink,
                                                fontSize = 11.sp,
                                                color = Color(0xFF757575),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                dateStr,
                                                fontSize = 10.sp,
                                                color = Color(0xFFBDBDBD),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color(0xFF9E9E9E))
            }
        }
    )
}

@Composable
fun LayoutPreviewDialog(
    item: LayoutItem,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var imageLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(item) {
        scale = 1f; offsetX = 0f; offsetY = 0f; imageLoaded = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // 整体渐入动画
        var dialogVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { dialogVisible = true }
        val dialogAlpha by animateFloatAsState(
            targetValue = if (dialogVisible) 1f else 0f,
            animationSpec = tween(250),
            label = "dialogAlpha"
        )

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = dialogAlpha },
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CocBg)
                        .clickable { onDismiss() }
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", tint = CocTextPrimary)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text("双指缩放单击返回", style = MaterialTheme.typography.titleMedium, color = CocTextSecondary)
                    Spacer(modifier = Modifier.weight(1f))
                    // 收藏按钮带动画
                    val favScale by animateFloatAsState(
                        targetValue = if (isFavorite) 1.2f else 1f,
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
                        label = "previewFavScale"
                    )
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "收藏",
                            tint = if (isFavorite) CocFavorite else CocTextHint,
                            modifier = Modifier.scale(favScale)
                        )
                    }
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, contentDescription = "分享", tint = CocTextSecondary)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (scale * zoom).coerceIn(1f, 5f)
                                scale = newScale
                                if (newScale > 1f) {
                                    val maxPan = 300f * (newScale - 1f)
                                    offsetX = (offsetX + pan.x).coerceIn(-maxPan, maxPan)
                                    offsetY = (offsetY + pan.y).coerceIn(-maxPan, maxPan)
                                } else {
                                    offsetX = 0f; offsetY = 0f
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (item.imageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(item.imageUrl)
                                .addHeader("Referer", "https://docs.qq.com/")
                                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                                .crossfade(true)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .build(),
                            contentDescription = "阵型",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .graphicsLayer(
                                    scaleX = scale, scaleY = scale,
                                    translationX = offsetX, translationY = offsetY
                                ),
                            contentScale = ContentScale.Fit,
                            onSuccess = { imageLoaded = true }
                        )
                    } else {
                        Text("暂无图片", color = CocTextHint)
                    }
                }
            }
        }
    }
}

@Composable
fun LayoutCard(
    item: LayoutItem,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onUse: (LayoutItem) -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current

    // 卡片按压效果
    var isCardPressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(
        targetValue = if (isCardPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "cardScale"
    )
    val cardElevation by animateDpAsState(
        targetValue = if (isCardPressed) 6.dp else 2.dp,
        animationSpec = tween(150),
        label = "cardElevation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isCardPressed = true
                        tryAwaitRelease()
                        isCardPressed = false
                    }
                )
            },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CocCard),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clickable(onClick = onClick)
            ) {
                if (item.imageUrl.isNotEmpty()) {
                    // 图片渐入加载
                    var imageAlpha by remember { mutableStateOf(0f) }
                    val animatedAlpha by animateFloatAsState(
                        targetValue = imageAlpha,
                        animationSpec = tween(400),
                        label = "imageAlpha"
                    )

                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(item.imageUrl)
                            .addHeader("Referer", "https://docs.qq.com/")
                            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                            .crossfade(true)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = "阵型",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                            .graphicsLayer { alpha = animatedAlpha },
                        contentScale = ContentScale.Crop,
                        onSuccess = { imageAlpha = 1f }
                    )
                    // 服务器标签
                    val serverLabel = buildString {
                        append(if (item.server == "en") "国际服" else "国服")
                        if (item.thLevel > 0) append(" TH${item.thLevel}")
                    }
                    Surface(
                        modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                        color = CocAccent.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = serverLabel,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    // 收藏按钮
                    val favScale by animateFloatAsState(
                        targetValue = if (isFavorite) 1.2f else 1f,
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
                        label = "cardFavScale"
                    )
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(36.dp)
                            .padding(4.dp)
                    ) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "收藏",
                            tint = if (isFavorite) CocFavorite else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(20.dp)
                                .scale(favScale)
                        )
                    }
                    // 放大镜标签
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp),
                        color = Color.Black.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(4.dp).size(16.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CocBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("暂无图片", color = CocTextHint, fontSize = 14.sp)
                    }
                }
            }
            // 按钮行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 使用按钮 - 带缩放
                val useInteraction = remember { MutableInteractionSource() }
                val isUsePressed by useInteraction.collectIsPressedAsState()
                val useScale by animateFloatAsState(
                    targetValue = if (isUsePressed) 0.92f else 1f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                    label = "useScale"
                )

                Surface(
                    onClick = { onUse(item) },
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .scale(useScale),
                    shape = RoundedCornerShape(6.dp),
                    color = CocAccent,
                    interactionSource = useInteraction
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "使用此阵型",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                val shareInteraction = remember { MutableInteractionSource() }
                val isSharePressed by shareInteraction.collectIsPressedAsState()
                val shareScale by animateFloatAsState(
                    targetValue = if (isSharePressed) 0.88f else 1f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                    label = "shareScale"
                )

                Surface(
                    onClick = onShare,
                    modifier = Modifier
                        .size(34.dp)
                        .scale(shareScale),
                    shape = RoundedCornerShape(6.dp),
                    color = CocAccentLight,
                    interactionSource = shareInteraction
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "分享",
                            tint = CocAccentDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
