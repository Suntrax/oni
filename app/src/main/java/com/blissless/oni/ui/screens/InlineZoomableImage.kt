package com.blissless.oni.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.launch

/**
 * A manga page rendered inside the vertical LazyColumn reader, with inline
 * pinch-zoom and double-tap-to-zoom support. No overlay, no separate screen —
 * the user zooms directly on the page they're already looking at.
 *
 * Gesture behaviour (designed to coexist with the parent LazyColumn's scroll):
 *
 *  - At 1× with a single finger:
 *      Vertical drag passes through to the LazyColumn, so the user can scroll
 *      the chapter normally. We deliberately do NOT consume the events here.
 *
 *  - Pinch (2 fingers) anywhere on the page:
 *      Continuous zoom in [1×, 6×], centered on the screen. Consumed here so
 *      the list doesn't scroll mid-pinch.
 *
 *  - Double-tap:
 *      If at 1×, animates to 2.5× and biases the offset so the tapped spot
 *      stays under the finger. If already zoomed in, animates back to 1×.
 *
 *  - Single-finger drag while zoomed in (>1×):
 *      Pans the zoomed image. Consumed here so the list doesn't scroll while
 *      the user is reading a zoomed panel.
 *
 * Per-page state (scale / offset) lives inside this composable, so when a page
 * scrolls out of view and is disposed by the LazyColumn, the next time it
 * scrolls back in it starts at 1× again. This matches the user's mental model:
 * "I zoomed in to read a panel, scrolled past it, came back — it should be at
 * normal zoom."
 *
 * Implementation note: we use `awaitEachGesture` rather than
 * `detectTransformGestures` because the latter always consumes drag events,
 * which would break single-finger scrolling at 1×. With `awaitEachGesture` we
 * can decide per-event whether to consume based on pointer count and current
 * zoom level.
 */
@Composable
fun InlineZoomableImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val scale = remember { Animatable(1f) }
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val density = LocalDensity.current

    // Build the ImageRequest with both memory and disk caching disabled —
    // same as [MihonZoomableImage]. See that composable's KDoc for the
    // rationale: chapter pages are never stored on-device.
    val imageRequest = remember(imageUrl) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .build()
    }

    // Pan limits. The exact bound depends on the rendered image's pixel size,
    // which we don't know until the AsyncImage has loaded. These values are
    // generous approximations based on a typical phone screen, scaled by
    // (zoom - 1) so higher zoom allows more pan range.
    val maxPanX = with(density) { 400.dp.toPx() }
    val maxPanY = with(density) { 800.dp.toPx() }

    fun clampOffset(x: Float, y: Float, s: Float): Pair<Float, Float> {
        if (s <= 1f) return 0f to 0f
        val limitX = maxPanX * (s - 1f)
        val limitY = maxPanY * (s - 1f)
        return x.coerceIn(-limitX, limitX) to y.coerceIn(-limitY, limitY)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            // Clip the zoomed image to the page's bounds so it doesn't bleed
            // into adjacent pages in the LazyColumn. Touch input is not
            // affected — only drawing.
            .clipToBounds()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tap ->
                        scope.launch {
                            if (scale.value > 1.01f) {
                                // Zoomed in → reset to 1×
                                scale.animateTo(1f, tween(220))
                                offsetX.animateTo(0f, tween(220))
                                offsetY.animateTo(0f, tween(220))
                            } else {
                                // At 1× → zoom to 2.5× centered on the tap point.
                                // We bias the offset so the spot the user tapped
                                // stays roughly under their finger after the zoom
                                // animation completes, which is the natural
                                // expectation for "zoom in here".
                                val target = 2.5f
                                val size = size
                                val nx = if (size.width > 0) tap.x / size.width else 0.5f
                                val ny = if (size.height > 0) tap.y / size.height else 0.5f
                                val dx = (nx - 0.5f) * (1f - target) * 600f
                                val dy = (ny - 0.5f) * (1f - target) * 800f
                                scale.animateTo(target, tween(220))
                                offsetX.animateTo(dx, tween(220))
                                offsetY.animateTo(dy, tween(220))
                            }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    do {
                        val event = awaitPointerEvent()
                        val pointers = event.changes.count { it.pressed }

                        // Consume pan/zoom only when:
                        //   - 2+ fingers are down (pinch), OR
                        //   - the page is already zoomed in (pan the image)
                        // At 1× with a single finger we deliberately DON'T
                        // consume, so the parent LazyColumn receives the event
                        // and scrolls the chapter list normally.
                        if (pointers >= 2 || scale.value > 1.01f) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val newScale = (scale.value * zoomChange).coerceIn(1f, 6f)
                            scope.launch {
                                scale.snapTo(newScale)
                                if (newScale > 1f) {
                                    val (cx, cy) = clampOffset(
                                        offsetX.value + panChange.x,
                                        offsetY.value + panChange.y,
                                        newScale
                                    )
                                    offsetX.snapTo(cx)
                                    offsetY.snapTo(cy)
                                } else {
                                    // Snapped back to 1× — reset offsets too so
                                    // the image is centered when the user lets go.
                                    offsetX.snapTo(0f)
                                    offsetY.snapTo(0f)
                                }
                            }
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxWidth()
                // graphicsLayer applies the zoom/pan as a GPU transform — much
                // cheaper than recomposing the AsyncImage, and smooth enough
                // for 60fps pinch animation.
                .graphicsLayer(
                    scaleX = scale.value,
                    scaleY = scale.value,
                    translationX = offsetX.value,
                    translationY = offsetY.value
                ),
            contentScale = ContentScale.FillWidth
        )
    }
}
