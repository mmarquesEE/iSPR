package com.example.ispr.ui.layouts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints

/**
 * Represents an individual tab configuration for the [TabsLayout].
 *
 * @property icon The [ImageVector] asset displayed in the [PrimaryTabRow] header.
 * @property content The Composable UI tree to render inside this specific tab's carousel pane.
 */
data class TabItem(
    val icon: ImageVector,
    val content: @Composable () -> Unit
)

/**
 * Layouts items side-by-side inside a single custom horizontal track.
 * Changing tabs applies a hardware-accelerated translation animation (`translationX`), keeping
 * interactions completely fluid at 60fps+ without triggering heavy recomposition passes.
 *
 * @param tabs Ordered list of [TabItem] elements containing header icons and screen contents.
 * @param modifier Optional [Modifier] applied to the outer host layout container.
 * @param initialTab The index of the tab that should be selected by default upon rendering.
 */
@Composable
fun TabsLayout(
    tabs: List<TabItem>,
    modifier: Modifier = Modifier,
    initialTab: Int = 0
) {
    // Tracks the currently active tab index
    var selectedTab by remember { mutableIntStateOf(initialTab) }

    // Smoothly animates the fractional step multiplier (e.g., from 0.0 to -1.0)
    // to drive the horizontal track's position.
    val translationOffset by animateFloatAsState(
        targetValue = -selectedTab.toFloat(),
        animationSpec = tween(durationMillis = 400),
        label = "CarouselTranslation"
    )

    Column(modifier = modifier.fillMaxSize()) {

        // ==========================================
        // 1. TAB HEADER NAVIGATION (Material 3)
        // ==========================================
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground
        ) {
            tabs.forEachIndexed { index, tabItem ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    icon = { Icon(tabItem.icon, contentDescription = null) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // ==========================================
        // 2. THE VISUAL CLIPPING VIEWPORT
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.background)
        ) {

            // ==========================================
            // 3. HORIZONTAL PRE-RENDERED CAROUSEL TRACK
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Performs a zero-recomposition translation on the render thread
                        translationX = translationOffset * size.width
                    }
            ) {
                tabs.forEach { tabItem ->
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            // Bypasses Row flex sizing to claim full viewport bounds
                            .then(FillParentWidthModifier)
                    ) {
                        // Invoked simultaneously for all tabs to ensure immediate readiness
                        tabItem.content()
                    }
                }
            }
        }
    }
}

/**
 * A custom layout modifier that forces a child element inside a standard [Row]
 * to bypass default sequential layout restrictions and strictly match 100% of the
 * parent's incoming maximum width and height layout constraints.
 */
private val FillParentWidthModifier = Modifier.layout { measurable, constraints ->
    val childConstraints = Constraints.fixed(
        width = constraints.maxWidth,
        height = constraints.maxHeight
    )
    val placeable = measurable.measure(childConstraints)

    layout(placeable.width, placeable.height) {
        placeable.place(0, 0)
    }
}