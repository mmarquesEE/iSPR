package com.example.ispr.ui.layouts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
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
    var selectedTab by remember { mutableIntStateOf(initialTab) }

    val translationOffset by animateFloatAsState(
        targetValue = -selectedTab.toFloat(),
        animationSpec = tween(durationMillis = 400),
        label = "CarouselTranslation"
    )

    Column(modifier = modifier.fillMaxSize()) {
        // 1. Tab Header
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

        // 2. Viewport (Clipping Container)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.background)
                .clipToBounds()
        ) {
            // 3. Carousel Track
            Layout(
                content = {
                    tabs.forEach { tabItem ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            tabItem.content()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = translationOffset * size.width
                    }
            ) { measurables, constraints ->
                val width = constraints.maxWidth
                val height = constraints.maxHeight
                
                // Use fixed constraints to ensure each tab is exactly the viewport size.
                // This prevents "squeezing" if the child content uses wrap-content widths.
                val childConstraints = Constraints.fixed(width, height)
                val movables = measurables.map { it.measure(childConstraints) }

                layout(width, height) {
                    movables.forEachIndexed { index, placeable ->
                        placeable.placeRelative(x = index * width, y = 0)
                    }
                }
            }
        }
    }
}
