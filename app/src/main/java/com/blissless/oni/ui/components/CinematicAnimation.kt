package com.blissless.oni.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val screenAnimations = mutableMapOf<String, ScreenAnimation>()

private class ScreenAnimation {
    val animatable = Animatable(0f)
    private var lastScreenKey: String = ""
    private var animationJob: Job? = null
    private var hasPlayedOnce = false

    fun startAnimation(scope: CoroutineScope, screenKey: String, playOncePerSession: Boolean = true) {
        if (animationJob?.isActive == true) return

        if (playOncePerSession && hasPlayedOnce && screenKey == lastScreenKey) {
            scope.launch(Dispatchers.Default) {
                animatable.snapTo(1f)
            }
            return
        }

        if (screenKey != lastScreenKey || !hasPlayedOnce) {
            lastScreenKey = screenKey
            hasPlayedOnce = true
            scope.launch(Dispatchers.Default) {
                animatable.snapTo(0f)
                animatable.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessVeryLow
                    )
                )
            }
        }
    }
}

@Composable
fun rememberCinematicAnimation(screenKey: String = "default", isVisible: Boolean = true, playOncePerSession: Boolean = true): Float {
    val animation = screenAnimations.getOrPut(screenKey) { ScreenAnimation() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(screenKey, isVisible) {
        if (isVisible) {
            animation.startAnimation(scope, screenKey, playOncePerSession)
        }
    }

    return animation.animatable.value
}

fun easeOutCubic(t: Float): Float {
    val t1 = t - 1
    return t1 * t1 * t1 + 1
}
