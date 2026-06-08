package com.nanan.coc

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
                                    onLayoutClick = { item -> openCocLayout(item.link, item.server) },
                                    onSelectServer = { viewModel.selectServer(it) },
                                    onSelectThLevel = { viewModel.selectThLevel(it) },
                                    onUpdateDocIds = { cnDoc, enDoc -> viewModel.updateDocIds(cnDoc, enDoc) },
                                    onSortModeChange = { viewModel.setSortMode(it) },
                                    onToggleFavorite = { link, imageUrl -> viewModel.toggleFavorite(link, imageUrl) },
                                    onToggleFavoritesOnly = { viewModel.toggleShowFavoritesOnly() },
                                    isFavorite = { viewModel.isFavorite(it) },
                                    onShare = { link -> shareLink(link) },
                                    onOpenCustomLink = { link, server -> openCocLayout(link, server) },
                                    onAddLinkHistory = { link, server, thLevel -> viewModel.addLinkHistory(link, server, thLevel) },
                                    onRemoveLinkHistory = { link -> viewModel.removeLinkHistory(link) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
