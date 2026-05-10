package com.nanan.coc.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.nanan.coc.R

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    state: AuthViewModel.AuthState
) {
    val context = LocalContext.current
    var kami by remember { mutableStateOf("") }
    var showDebug by remember { mutableStateOf(false) }
    var debugText by remember { mutableStateOf("") }
    var showNotice by remember { mutableStateOf(false) }
    var noticeText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                null -> {}
                is AuthViewModel.UiEvent.Toast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
                is AuthViewModel.UiEvent.ShowDebug -> {
                    debugText = event.message
                    showDebug = true
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

    if (showDebug) {
        val clipboardManager = LocalClipboardManager.current
        AlertDialog(
            onDismissRequest = { showDebug = false },
            title = { Text("调试信息") },
            text = {
                Text(
                    text = debugText,
                    fontSize = 10.sp,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        clipboardManager.setText(AnnotatedString(debugText))
                        Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                    }) { Text("复制") }
                    TextButton(onClick = { showDebug = false }) { Text("关闭") }
                }
            }
        )
    }

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

            // App 图标
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher),
                contentDescription = "图标",
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "COC阵型工具",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF404040),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "输入卡密以激活应用",
                fontSize = 14.sp,
                color = Color(0xFFA0A0A0),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 输入卡
            Surface(
                modifier = Modifier.fillMaxWidth(),
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

                    Button(
                        onClick = { viewModel.login(kami) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = state != AuthViewModel.AuthState.LoggingIn,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF505050),
                            disabledContainerColor = Color(0xFFC0C0C0)
                        )
                    ) {
                        if (state == AuthViewModel.AuthState.LoggingIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
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

            // 无卡密模式
            TextButton(
                onClick = { viewModel.enterGuest() },
                enabled = state != AuthViewModel.AuthState.LoggingIn
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
                enabled = state != AuthViewModel.AuthState.LoggingIn
            ) {
                Text(
                    "解绑设备",
                    fontSize = 13.sp,
                    color = if (state != AuthViewModel.AuthState.LoggingIn)
                        Color(0xFFA0A0A0) else Color(0xFFD0D0D0)
                )
            }

            if (state == AuthViewModel.AuthState.NeedLogin && debugText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { showDebug = true }) {
                    Text("查看调试日志", fontSize = 12.sp, color = Color(0xFFB0B0B0))
                }
            }

            // 底部留白
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
