package com.zeroui.kit.components

import com.zeroui.kit.motion.LocalReducedMotion
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme


private val SteadyAlpha: State<Float> = mutableFloatStateOf(1f)


@Composable
fun BreathingDot(
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.primary,
) {
    val alpha = if (LocalReducedMotion.current) SteadyAlpha else breathingAlpha()
    Spacer(
        modifier = modifier.drawBehind {
            drawCircle(color = color.copy(alpha = alpha.value))
        }
    )
}

@Composable
private fun breathingAlpha(): State<Float> =
    rememberInfiniteTransition(label = "breathingDot").animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathingDotAlpha",
    )
