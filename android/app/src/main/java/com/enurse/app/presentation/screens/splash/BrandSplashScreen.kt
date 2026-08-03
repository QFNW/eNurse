package com.enurse.app.presentation.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.enurse.app.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Brand splash: transparent-background logo scales up + fades in over a
 * vertical gradient sampled from pics/startpagelogo.png (top #7AE2EA →
 * bottom #72DEE8), so the logo's own cut-out background blends seamlessly.
 * Calls [onFinished] after ~1.2s. The delayed timeout is a separate coroutine
 * so the transition can never stall even if an animation fails.
 */
@Composable
fun BrandSplashScreen(
    logoRes: Int = R.drawable.splash_logo,
    backgroundGradient: Brush = Brush.verticalGradient(
        listOf(Color(0xFF7AE2EA), Color(0xFF72DEE8))
    ),
    onFinished: () -> Unit
) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.55f) }

    // Animation is best-effort; the absolute timeout on a separate coroutine
    // guarantees onFinished fires even if an animation throws or hangs.
    LaunchedEffect(Unit) {
        launch {
            runCatching {
                coroutineScope {
                    launch { alpha.animateTo(1f, tween(650)) }
                    launch { scale.animateTo(1f, tween(850, easing = FastOutSlowInEasing)) }
                }
            }
        }
        delay(1400)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(logoRes),
            contentDescription = null,
            modifier = Modifier
                .size(240.dp)
                .graphicsLayer {
                    this.alpha = alpha.value
                    scaleX = scale.value
                    scaleY = scale.value
                }
        )
    }
}
