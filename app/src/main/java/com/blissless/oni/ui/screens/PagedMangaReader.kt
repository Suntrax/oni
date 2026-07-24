package com.blissless.oni.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.toSize
import com.blissless.oni.data.ReaderMode
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Paged manga reader (Mihon-style).
 *
 * Renders one page per screen inside a HorizontalPager. The reading direction
 * is controlled by `mode`:
 *
 *  - LEFT_TO_RIGHT: the next page is to the right; swiping leftward reveals it.
 *      This is the "western comics" direction.
 *  - RIGHT_TO_LEFT: the next page is to the left; swiping rightward reveals it.
 *      This is the manga-traditional direction and matches Mihon's default for
 *      Japanese-language titles.
 *
 * Tap behaviour:
 *  - Single tap on the leftmost ~20% (LTR) or rightmost ~20% (RTL) of the
 *    screen → previous page (gives the user a way back without swiping).
 *  - Single tap anywhere else → next page.
 *
 * Double-tap and pinch behaviour come from MihonZoomableImage. When the image
 * is zoomed in, the zoomable image consumes drag events so the pager doesn't
 * accidentally flip pages while the user is panning. The pager's swipe gesture
 * is also disabled while zoomed (see `userScrollEnabled` below).
 *
 * The page-change callback is throttled to one call per page transition via
 * `distinctUntilChanged`, so the host ReaderScreen can update scroll progress
 * / chapter-mark-as-read logic exactly once per page.
 */
@Composable
fun PagedMangaReader(
    images: List<String>,
    initialPage: Int,
    mode: ReaderMode,
    chapterTitle: String?,
    onPageChanged: (pageIndex: Int) -> Unit,
    onChapterBoundary: (direction: Int) -> Unit,
    onMiddleTap: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (images.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("No pages to display", color = Color.White)
        }
        return
    }

    val isRtl = mode == ReaderMode.RIGHT_TO_LEFT
    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, images.lastIndex),
        pageCount = { images.size }
    )
    val scope = rememberCoroutineScope()

    // Track which page the user is currently on, fire onPageChanged exactly
    // once per transition.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page -> onPageChanged(page) }
    }

    // We track the current page's zoom state via this flag, which the inner
    // MihonZoomableImage updates through the onZoomChanged callback. While
    // zoomed in, the HorizontalPager's swipe-to-paginate is disabled so the
    // user can pan freely without flipping pages.
    var currentPageZoomed by remember { mutableStateOf(false) }

    // Per-page double-tap zoom trigger. Incremented when a double-tap is
    // detected at the pager level (before HorizontalPager consumes the event).
    // MihonZoomableImage watches this and toggles zoom accordingly.
    val doubleTapTriggers = remember { mutableStateMapOf<Int, Int>() }

    Box(modifier = modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            reverseLayout = isRtl,
            userScrollEnabled = !currentPageZoomed,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                // Detect double-taps at the pager level using Initial pass,
                // which runs BEFORE HorizontalPager's own gesture detection.
                // This is necessary because the HorizontalPager intercepts
                // touch events before MihonZoomableImage's detectTapGestures
                // can see them.
                .pointerInput(pagerState.currentPage) {
                    awaitEachGesture {
                        // Wait for first tap down.
                        val down1 = awaitFirstDown(requireUnconsumed = false)
                        val t1 = down1.uptimeMillis
                        // Wait for it to be released.
                        waitForUpOrCancellation() ?: return@awaitEachGesture
                        // Wait for second tap down within the double-tap window.
                        val down2 = awaitFirstDown(requireUnconsumed = false)
                        val t2 = down2.uptimeMillis
                        if (t2 - t1 < 300) {
                            // Double-tap detected — increment the trigger for
                            // the current page so MihonZoomableImage toggles zoom.
                            val page = pagerState.currentPage
                            doubleTapTriggers[page] = (doubleTapTriggers[page] ?: 0) + 1
                            // Consume the second tap so the pager doesn't also
                            // process it as a single tap / swipe.
                            down2.consume()
                        }
                    }
                }
        ) { pageIndex ->
        // Reset the zoom flag when the user lands on a new page: the new page
        // starts at 1×, so the pager should be swipeable until the user pinches.
        var isZoomed by remember(pageIndex) { mutableStateOf(false) }
        // Track the laid-out size so we can divide the screen into tap zones.
        var pageLayoutSize by remember(pageIndex) { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

        // The "current" page (the one centered on screen) is what controls
        // `userScrollEnabled`. We update `currentPageZoomed` whenever the
        // in-view page's zoom flag changes.
        LaunchedEffect(pagerState.currentPage, isZoomed) {
            currentPageZoomed = if (pagerState.currentPage == pageIndex) isZoomed else false
        }

        MihonZoomableImage(
            imageUrl = images[pageIndex],
            contentDescription = "Page ${pageIndex + 1}",
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { pageLayoutSize = it.toSize() },
            fillWidth = false,
            doubleTapTrigger = doubleTapTriggers[pageIndex] ?: 0,
            onSingleTap = { tap ->
                if (isZoomed) return@MihonZoomableImage

                val width = pageLayoutSize.width
                if (width <= 0f) return@MihonZoomableImage

                val isMiddleZone = tap.x > width * 0.3f && tap.x < width * 0.7f
                if (isMiddleZone) {
                    onMiddleTap()
                    return@MihonZoomableImage
                }

                val isPrevZone = if (isRtl) tap.x > width * 0.7f else tap.x < width * 0.3f

                scope.launch {
                    when {
                        isPrevZone && pagerState.currentPage > 0 ->
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        !isPrevZone && pagerState.currentPage < images.lastIndex ->
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        !isPrevZone && pagerState.currentPage == images.lastIndex ->
                            onChapterBoundary(1)
                        isPrevZone && pagerState.currentPage == 0 ->
                            onChapterBoundary(-1)
                    }
                }
            },
            onZoomChanged = { zoomed -> isZoomed = zoomed }
            )
        }
    }
}
