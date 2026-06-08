package com.nanan.coc.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import android.widget.Toast
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.platform.LocalContext
import com.nanan.coc.R

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    state: AuthViewModel.AuthState
) {
    val context = LocalContext.current
    var kami by remember { mutableStateOf("") }
    var showNotice by remember { mutableStateOf(false) }
    var noticeText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                null -> {}
                is AuthViewModel.UiEvent.Toast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }

                is AuthViewModel.UiEvent.ShowNotice -> {
                    noticeText = event.message
                    showNotice = true
                }
            }
            viewModel.clearEvent()
        }
    }

    if (showNotice) {
        AlertDialog(
            onDismissRequest = { showNotice = false },
            title = { Text("公告") },
            text = {
                Text(
                    text = noticeText,
                    fontSize = 14.sp,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            },
            confirmButton = {
                TextButton(onClick = { showNotice = false }) { Text("朕已阅") }
            }
        )
    }

    // 入场动画：Logo 缩放 + 表单渐入
    var animationPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animationPlayed = true }

    val logoScale by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0.3f,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 200f
        ),
        label = "logoScale"
    )

    val logoAlpha by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(500),
        label = "logoAlpha"
    )

    val formAlpha by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(600, delayMillis = 200),
        label = "formAlpha"
    )

    val formOffsetY by animateFloatAsState(
        targetValue = if (animationPlayed) 0f else 30f,
        animationSpec = tween(600, delayMillis = 200, easing = FastOutSlowInEasing),
        label = "formOffsetY"
    )

    val footerAlpha by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(500, delayMillis = 400),
        label = "footerAlpha"
    )

    Scaffold(
        containerColor = Color.White,
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // App 图标 - 弹性缩放入场
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher),
                contentDescription = "图标",
                modifier = Modifier
                    .size(80.dp)
                    .scale(logoScale)
                    .graphicsLayer { alpha = logoAlpha }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "COC阵型工具",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF404040),
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = logoAlpha
                    translationY = formOffsetY * 0.3f
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "输入卡密以激活应用",
                fontSize = 14.sp,
                color = Color(0xFFA0A0A0),
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = logoAlpha }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 输入卡 - 滑入入场
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = formAlpha
                        translationY = formOffsetY
                    },
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFAFAFA),
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = kami,
                        onValueChange = { kami = it },
                        placeholder = { Text("请输入卡密", color = Color(0xFFC0C0C0)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF808080),
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            cursorColor = Color(0xFF606060)
                        ),
                        enabled = state != AuthViewModel.AuthState.LoggingIn
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 激活按钮 - 带按压缩放
                    val btnInteraction = remember { MutableInteractionSource() }
                    val isBtnPressed by btnInteraction.collectIsPressedAsState()
                    val btnScale by animateFloatAsState(
                        targetValue = if (isBtnPressed) 0.95f else 1f,
                        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                        label = "loginBtnScale"
                    )

                    Button(
                        onClick = { viewModel.login(kami) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .scale(btnScale),
                        enabled = state != AuthViewModel.AuthState.LoggingIn,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF505050),
                            disabledContainerColor = Color(0xFFC0C0C0)
                        ),
                        interactionSource = btnInteraction
                    ) {
                        if (state == AuthViewModel.AuthState.LoggingIn) {
                            // 加载中 - 旋转动画
                            val rotation by rememberInfiniteTransition(label = "loadingRot").animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "loadingRotation"
                            )
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer { rotationZ = rotation },
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("验证中...", fontSize = 15.sp)
                        } else {
                            Text("激活", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 无卡密模式 - 延迟渐入
            TextButton(
                onClick = { viewModel.enterGuest() },
                enabled = state != AuthViewModel.AuthState.LoggingIn,
                modifier = Modifier.graphicsLayer { alpha = footerAlpha }
            ) {
                Text(
                    "无卡密模式",
                    fontSize = 13.sp,
                    color = Color(0xFF808080)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            TextButton(
                onClick = { viewModel.unbind(kami) },
                enabled = state != AuthViewModel.AuthState.LoggingIn,
                modifier = Modifier.graphicsLayer { alpha = footerAlpha }
            ) {
                Text(
                    "解绑设备",
                    fontSize = 13.sp,
                    color = if (state != AuthViewModel.AuthState.LoggingIn)
                        Color(0xFFA0A0A0) else Color(0xFFD0D0D0)
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
