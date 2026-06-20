package com.nature.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.nature.browser.TabModel

import androidx.lifecycle.viewmodel.compose.viewModel
import com.nature.browser.BrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartAddressBar(
    tab: TabModel,
    onUrlSubmit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: BrowserViewModel = viewModel()
    val url by tab.url.collectAsState()
    val isSecure by tab.isSecure.collectAsState()
    val progress by tab.progress.collectAsState()
    var textInput by remember(url) { mutableStateOf(url) }
    val suggestions by viewModel.suggestions.collectAsState()
    var isFocused by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val leafScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "leafScale"
    )

    // River stone shape
    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            tonalElevation = 2.dp
        ) {
            TextField(
                value = textInput,
                onValueChange = {
                    textInput = it
                    viewModel.updateSuggestions(it)
                },
                modifier = Modifier.fillMaxWidth().onFocusChanged { isFocused = it.isFocused },
            placeholder = { Text("Search or enter URL", style = MaterialTheme.typography.bodyMedium) },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            leadingIcon = {
                if (url.isNotEmpty() && url != "about:blank") {
                    val leafModifier = if (progress in 1..99) {
                        Modifier.size(20.dp).scale(leafScale)
                    } else {
                        Modifier.size(20.dp)
                    }

                    Icon(
                        imageVector = when {
                            isSecure -> Icons.Default.Eco
                            url.startsWith("https") -> Icons.Default.Eco
                            progress == 100 -> Icons.Default.HeartBroken
                            else -> Icons.Default.Eco
                        },
                        contentDescription = "Security Status",
                        tint = when {
                            isSecure -> Color(0xFF57CC99) // Green leaf (Verified HTTPS)
                            url.startsWith("https") -> Color(0xFFFFD166) // Yellow leaf (Mixed or Encrypted)
                            progress == 100 -> Color(0xFFE76F51) // Broken leaf (HTTP)
                            else -> Color(0xFF57CC99).copy(alpha = 0.6f)
                        },
                        modifier = leafModifier
                    )
                }
            },
            trailingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(20.dp))
            },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    onUrlSubmit(textInput)
                    isFocused = false
                })
            )
        }

        if (isFocused && suggestions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    suggestions.take(5).forEach { suggestion ->
                        ListItem(
                            headlineContent = { Text(suggestion.title, maxLines = 1) },
                            supportingContent = { Text(suggestion.url, maxLines = 1, style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.clickable {
                                textInput = suggestion.url
                                onUrlSubmit(suggestion.url)
                                isFocused = false
                            }
                        )
                    }
                }
            }
        }
    }
}
