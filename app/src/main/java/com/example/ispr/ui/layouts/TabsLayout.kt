package com.example.ispr.ui.layouts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.vector.ImageVector

data class TabItem(
    val icon: ImageVector,
    val content: @Composable () -> Unit
)

@Composable
fun TabsLayout(
    tabs: List<TabItem>,
    modifier: Modifier = Modifier,
    initialTab: Int = 0
){
    var selectedTab by remember { mutableIntStateOf(initialTab) }

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryTabRow(
            selectedTab,
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ){
            if(selectedTab in tabs.indices) {
                tabs[selectedTab].content()
            }
        }

    }
}