package com.nanan.coc.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shimmer 骨架屏加载效果
 */
@Composable
fun ShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerColors = listOf(
        Color(0xFFE0E0E0),
        Color(0xFFF5F5F5),
        Color(0xFFE0E0E0),
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )
}

/**
 * 阵型卡片骨架屏
 */
@Composable
fun LayoutCardShimmer(modifier: Modifier = Modifier) {
    val brush = ShimmerBrush()

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
    ) {
        // 图片区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(brush)
        )
        // 按钮区域
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(brush)
            )
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(brush)
            )
        }
    }
}

/**
 * 筛选标签骨架屏
 */
@Composable
fun ChipRowShimmer(modifier: Modifier = Modifier) {
    val brush = ShimmerBrush()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(brush)
            )
        }
    }
}

/**
 * 完整的阵型列表骨架屏
 */
@Composable
fun LayoutGridShimmer(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 公告行
        ChipRowShimmer()
        // 服务器筛选
        ChipRowShimmer()
        // TH等级筛选
        ChipRowShimmer()
        Spacer(modifier = Modifier.height(4.dp))
        // 卡片网格 - 2列
        val brush = ShimmerBrush()
        repeat(3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LayoutCardShimmer(modifier = Modifier.weight(1f))
                LayoutCardShimmer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
