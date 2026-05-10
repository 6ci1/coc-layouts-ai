package com.nanan.coc

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nanan.coc.auth.AuthScreen
import com.nanan.coc.auth.AuthViewModel
import com.nanan.coc.ui.screen.LayoutGridScreen
import com.nanan.coc.ui.viewmodel.LayoutViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        const val COC_CN = "com.tencent.tmgp.supercell.clashofclans"
        const val COC_EN = "com.supercell.clashofclans"
    }

    private fun isPackageInstalled(pkg: String): Boolean {
        return try {
            packageManager.getPackageInfo(pkg, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun openCocLayout(link: String, server: String) {
        val uri = Uri.parse(link)
        val pkg = if (server == "en") COC_EN else COC_CN

        if (!isPackageInstalled(pkg)) {
            val name = if (server == "en") "国际服" else "国服"
            Toast.makeText(this, "未安装${name}部落冲突", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.action = Intent.ACTION_VIEW
                launchIntent.data = uri
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(launchIntent)
                return
            }
        } catch (_: Exception) {}

        try {
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage(pkg)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            return
        } catch (_: Exception) {}

        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareLink(link: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, link)
        }
        startActivity(Intent.createChooser(intent, "分享阵型"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val authViewModel: AuthViewModel = hiltViewModel()
            val authState by authViewModel.state.collectAsState()

            // 启动时检查验证状态
            LaunchedEffect(Unit) {
                authViewModel.checkAuth()
            }

            when (authState) {
                is AuthViewModel.AuthState.Checking -> {
                    // 检查中：空白或加载指示
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF909090))
                    }
                }

                is AuthViewModel.AuthState.NeedLogin,
                is AuthViewModel.AuthState.LoggingIn -> {
                    AuthScreen(viewModel = authViewModel, state = authState)
                }

                is AuthViewModel.AuthState.Activated -> {
                    MainContent(
                        onOpenCocLayout = { link, server -> openCocLayout(link, server) },
                        onShareLink = { link -> shareLink(link) },
                        onCheckHeartbeat = { authViewModel.checkHeartbeat() }
                    )
                }

                is AuthViewModel.AuthState.Guest -> {
                    GuestContent(
                        onOpenCocLayout = { link, server -> openCocLayout(link, server) },
                        onActivate = { authViewModel.exitGuest() }
                    )
                }
            }
        }
    }

    @Composable
    private fun MainContent(
        onOpenCocLayout: (String, String) -> Unit,
        onShareLink: (String) -> Unit,
        onCheckHeartbeat: () -> Unit = {}
    ) {
        val cocScheme = lightColorScheme(
            primary = Color(0xFF909090),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE9E9E9),
            onPrimaryContainer = Color(0xFF707070),
            secondary = Color(0xFF909090),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFE9E9E9),
            onSecondaryContainer = Color(0xFF909090),
            tertiary = Color(0xFFBDBDBD),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFE9E9E9),
            onTertiaryContainer = Color(0xFF909090),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF404040),
            onSurfaceVariant = Color(0xFF909090),
            error = Color(0xFFEF5350),
            outline = Color(0xFFE9E9E9),
            outlineVariant = Color(0xFFE9E9E9)
        )

        MaterialTheme(colorScheme = cocScheme) {
            Surface(color = MaterialTheme.colorScheme.surface) {
                val navController = rememberNavController()

                // 每 2 分钟心跳验证一次卡密是否仍有效
                LaunchedEffect(Unit) {
                    while (true) {
                        kotlinx.coroutines.delay(120_000) // 2分钟
                        onCheckHeartbeat()
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = "layouts"
                    ) {
                        composable("layouts") {
                            val viewModel: LayoutViewModel = hiltViewModel()
                            val uiState by viewModel.uiState.collectAsState()

                            LayoutGridScreen(
                                uiState = uiState,
                                onRefresh = { viewModel.refresh() },
                                onLayoutClick = { item -> onOpenCocLayout(item.link, item.server) },
                                onSelectServer = { viewModel.selectServer(it) },
                                onSelectThLevel = { viewModel.selectThLevel(it) },
                                onUpdateDocIds = { cnDoc, enDoc -> viewModel.updateDocIds(cnDoc, enDoc) },
                                onSortModeChange = { viewModel.setSortMode(it) },
                                onToggleFavorite = { link, imageUrl -> viewModel.toggleFavorite(link, imageUrl) },
                                onToggleFavoritesOnly = { viewModel.toggleShowFavoritesOnly() },
                                isFavorite = { viewModel.isFavorite(it) },
                                onShare = { link -> onShareLink(link) },
                                onOpenCustomLink = { link, server -> onOpenCocLayout(link, server) },
                                onAddLinkHistory = { link, server, thLevel -> viewModel.addLinkHistory(link, server, thLevel) },
                                onRemoveLinkHistory = { link -> viewModel.removeLinkHistory(link) }
                            )
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun GuestContent(
        onOpenCocLayout: (String, String) -> Unit,
        onActivate: () -> Unit
    ) {
        var link by remember { mutableStateOf("") }
        var linkHistory by remember { mutableStateOf<List<Pair<String, Long>>>(emptyList()) }
        val prefs = remember { getSharedPreferences("coc_guest", MODE_PRIVATE) }
        val context = androidx.compose.ui.platform.LocalContext.current

        // Load history
        LaunchedEffect(Unit) {
            val history = mutableListOf<Pair<String, Long>>()
            val set = prefs.getStringSet("guest_link_history", emptySet()) ?: emptySet()
            set.sortedByDescending { it.split("|")[0].toLongOrNull() ?: 0 }.forEach { entry ->
                val parts = entry.split("|", limit = 2)
                if (parts.size == 2) {
                    val time = parts[0].toLongOrNull() ?: return@forEach
                    val savedLink = prefs.getString("guest_link_${parts[1]}", null) ?: return@forEach
                    history.add(Pair(savedLink, time))
                }
            }
            linkHistory = history
        }

        val hasLink = link.isNotBlank()
        val prefix = "https://link.clashofclans.com/"
        val darkGray = Color(0xFF757575)
        val lightGray = Color(0xFFE0E0E0)

        Scaffold(
            containerColor = Color.White,
            contentWindowInsets = WindowInsets(0.dp),
            topBar = {
                TopAppBar(
                    title = { },
                    actions = {
                        TextButton(onClick = {
                            Toast.makeText(context, "此APP仅为部落成员使用", Toast.LENGTH_SHORT).show()
                            onActivate()
                        }) {
                            Text("输入卡密激活", fontSize = 13.sp, color = Color(0xFF505050))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                    windowInsets = WindowInsets.statusBars
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher),
                    contentDescription = "图标",
                    modifier = Modifier.size(72.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "COC阵型工具",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF404040)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "无卡密模式 — 仅可使用自定义链接",
                    fontSize = 13.sp,
                    color = Color(0xFFA0A0A0)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Link input
                OutlinedTextField(
                    value = link,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.startsWith(prefix) || prefix.startsWith(newValue)) {
                            link = newValue
                        }
                    },
                    placeholder = { Text("粘贴阵型链接...", color = Color(0xFFBDBDBD)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = darkGray,
                        unfocusedBorderColor = lightGray,
                        cursorColor = darkGray
                    ),
                    trailingIcon = {
                        if (link.isNotEmpty()) {
                            IconButton(onClick = { link = "" }, modifier = Modifier.size(20.dp)) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "清除",
                                    tint = Color(0xFFBDBDBD),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Open buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        onClick = {
                            if (hasLink) {
                                val l = link.trim()
                                saveGuestLinkHistory(prefs, l)
                                onOpenCocLayout(l, "cn")
                            }
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = if (hasLink) darkGray else lightGray,
                        enabled = hasLink
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "国服打开",
                                color = if (hasLink) Color.White else Color(0xFF9E9E9E),
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    }
                    Surface(
                        onClick = {
                            if (hasLink) {
                                val l = link.trim()
                                saveGuestLinkHistory(prefs, l)
                                onOpenCocLayout(l, "en")
                            }
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = if (hasLink) darkGray else lightGray,
                        enabled = hasLink
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "国际服打开",
                                color = if (hasLink) Color.White else Color(0xFF9E9E9E),
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // History
                if (linkHistory.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        "历史记录",
                        fontSize = 12.sp,
                        color = Color(0xFF9E9E9E),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    linkHistory.take(20).forEach { (historyLink, time) ->
                        val display = if (historyLink.length > 50) historyLink.take(47) + "..." else historyLink
                        val timeStr = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
                            .format(java.util.Date(time))
                        Surface(
                            onClick = { onOpenCocLayout(historyLink, "cn") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFAFAFA)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(display, fontSize = 12.sp, color = Color(0xFF606060), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(timeStr, fontSize = 10.sp, color = Color(0xFFBDBDBD))
                                }
                                TextButton(onClick = {
                                    removeGuestLinkHistory(prefs, historyLink)
                                    linkHistory = linkHistory.filter { it.first != historyLink }
                                }) {
                                    Text("删除", fontSize = 11.sp, color = Color(0xFFBDBDBD))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    private fun saveGuestLinkHistory(prefs: android.content.SharedPreferences, link: String) {
        val hashCode = link.hashCode().toString()
        prefs.edit().putString("guest_link_$hashCode", link).apply()
        val set = (prefs.getStringSet("guest_link_history", emptySet()) ?: emptySet()).toMutableSet()
        set.removeAll { it.endsWith("|$hashCode") }
        set.add("${System.currentTimeMillis()}|$hashCode")
        prefs.edit().putStringSet("guest_link_history", set.take(50).toSet()).apply()
    }

    private fun removeGuestLinkHistory(prefs: android.content.SharedPreferences, link: String) {
        val hashCode = link.hashCode().toString()
        val set = (prefs.getStringSet("guest_link_history", emptySet()) ?: emptySet()).toMutableSet()
        set.removeAll { it.endsWith("|$hashCode") }
        prefs.edit()
            .putStringSet("guest_link_history", set)
            .remove("guest_link_$hashCode")
            .apply()
    }
}
