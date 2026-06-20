package com.nature.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nature.browser.TabModel
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color

@Composable
fun SplitScreenContainer(
    topTab: TabModel,
    bottomTab: TabModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            GeckoViewWrapper(tab = topTab, modifier = Modifier.fillMaxSize())
        }

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(Color(0xFF2A9D8F))
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            GeckoViewWrapper(tab = bottomTab, modifier = Modifier.fillMaxSize())
        }
    }
}
