package com.blissless.oni.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    HorizontalPager(
        state = pagerState,
        // RTL layout: Compose's HorizontalPager supports this directly via the
        // `reverseLayout` parameter. When reversed, page 0 starts on the right
        // and swiping rightward moves to page 1 — exactly what manga readers
        // expect.
        reverseLayout = isRtl,
        // Disable the pager's own swipe while the current page is zoomed in,
        // so the user can pan across the zoomed image without flipping pages.
        userScrollEnabled = !currentPageZoomed,
        modifier = modifier.fillMaxSize().background(Color.Black)
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
            onSingleTap = { tap ->
                // If the image is zoomed in, the user is reading a panel —
                // swallow the tap so we don't flip pages on them. They can
                // double-tap to zoom out first, then single-tap to advance.
                if (isZoomed) return@MihonZoomableImage

                val width = pageLayoutSize.width
                if (width <= 0f) return@MihonZoomableImage

                // Use a 20% edge zone for "previous page" so users can go back
                // without swiping. The edge side depends on reading direction:
                // in LTR, the left edge is "back"; in RTL, the right edge is.
                val isPrevZone = if (isRtl) tap.x > width * 0.8f else tap.x < width * 0.2f

                scope.launch {
                    when {
                        isPrevZone && pagerState.currentPage > 0 ->
                            // Smooth animated page transition using the pager's
                            // default spring. This is the single-arg overload
                            // `animateScrollToPage(page: Int)` — the two-arg
                            // overload that takes a custom AnimationSpec is not
                            // available in this Compose BOM version, but the
                            // default spring is well-tuned for paging and feels
                            // smooth. Swipe gestures also use this same spring
                            // internally, so taps and swipes feel consistent.
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        !isPrevZone && pagerState.currentPage < images.lastIndex ->
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        // Past the last page → ask the host for the next chapter.
                        !isPrevZone && pagerState.currentPage == images.lastIndex ->
                            onChapterBoundary(1)
                        // Before the first page → ask the host for the prev chapter.
                        isPrevZone && pagerState.currentPage == 0 ->
                            onChapterBoundary(-1)
                    }
                }
            },
            onZoomChanged = { zoomed -> isZoomed = zoomed }
        )
    }
}
