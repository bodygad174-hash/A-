package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AfhCyan
import com.example.ui.theme.AfhElectricBlue
import com.example.ui.theme.AfhSoftPurple
import com.example.ui.theme.AfhTurquoise
import com.example.ui.theme.SpaceGroteskFamily

@Composable
fun AfhLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    animated: Boolean = false
) {
    val scale = if (animated) {
        val infiniteTransition = rememberInfiniteTransition(label = "afh_logo_pulse")
        val anim by infiniteTransition.animateFloat(
            initialValue = 0.97f,
            targetValue = 1.03f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "logo_scale"
        )
        anim
    } else 1f

    val shape = RoundedCornerShape(size * 0.26f)
    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(shape)
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(listOf(AfhCyan, AfhElectricBlue, AfhSoftPurple)),
                shape = shape
            )
    ) {
        Image(
            painter = painterResource(id = R.drawable.afh_hero_artwork),
            contentDescription = "AFH AI Logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun AfhHorizontalLogo(
    modifier: Modifier = Modifier,
    iconSize: Dp = 36.dp,
    showSlogan: Boolean = false,
    sloganText: String = "One Platform. Your AI. Your Identity.",
    animated: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AfhLogoMark(size = iconSize, animated = animated)
        Spacer(modifier = Modifier.width(10.dp))
        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                    ) { append("AFH ") }
                    withStyle(
                        SpanStyle(
                            color = AfhCyan,
                            fontWeight = FontWeight.Bold
                        )
                    ) { append("AI") }
                },
                fontFamily = SpaceGroteskFamily,
                fontSize = 20.sp,
                letterSpacing = 1.2.sp
            )
            if (showSlogan) {
                Text(
                    text = sloganText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AfhGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
        ),
        tonalElevation = 4.dp,
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier.background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                        Color.Transparent
                    )
                )
            )
        ) {
            content()
        }
    }
}

fun afhModelIcon(iconKey: String): ImageVector = when (iconKey) {
    "flash" -> Icons.Rounded.Bolt
    "pro" -> Icons.Rounded.Psychology
    "code" -> Icons.Rounded.Code
    "study" -> Icons.Rounded.School
    "creative" -> Icons.Rounded.Palette
    "vision" -> Icons.Rounded.Visibility
    "research" -> Icons.Rounded.Science
    else -> Icons.Rounded.AutoAwesome
}

fun afhModelAccentColor(iconKey: String): Color = when (iconKey) {
    "flash" -> AfhCyan
    "pro" -> AfhElectricBlue
    "code" -> AfhTurquoise
    "study" -> Color(0xFF47B5FF)
    "creative" -> AfhSoftPurple
    "vision" -> Color(0xFF28D8E6)
    "research" -> Color(0xFF7B88FF)
    else -> AfhElectricBlue
}
