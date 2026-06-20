package com.nature.browser.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.*

@Composable
fun NewTabPage(
    onShortcutClick: (String) -> Unit,
    onSearch: (String) -> Unit
) {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)

    val (topColor, bottomColor) = when (hour) {
        in 5..8 -> Color(0xFFFFD166) to Color(0xFFF5F9F5) // Dawn
        in 9..16 -> Color(0xFF48CAE4) to Color(0xFFF0FAF8) // Morning/Afternoon
        in 17..19 -> Color(0xFFE76F51) to Color(0xFF264653) // Dusk
        else -> Color(0xFF1A1423) to Color(0xFF0D211F) // Night
    }

    val animatedTopColor by animateColorAsState(topColor, tween(2000), label = "top")
    val animatedBottomColor by animateColorAsState(bottomColor, tween(2000), label = "bottom")

    val quote = remember {
        val quotes = listOf(
            "Look deep into nature, and then you will understand everything better.",
            "Nature always wears the colors of the spirit.",
            "The earth has music for those who listen.",
            "In every walk with nature one receives far more than he seeks."
        )
        quotes.random()
    }

    var shortcuts by remember { mutableStateOf(listOf("Google" to "https://www.google.com", "Wikipedia" to "https://www.wikipedia.org")) }
    var showAddDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(animatedTopColor.copy(alpha = 0.3f), animatedBottomColor)))
    ) {
        // Nature Scene
        Canvas(modifier = Modifier.fillMaxSize()) {
            val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

            // Multiple layers of hills for depth with organic watercolor effect
            val layers = 9
            for (i in 1..layers) {
                val path = Path().apply {
                    val startY = size.height * (0.5f + (i * 0.06f))
                    moveTo(0f, startY)
                    // More organic and varied cubic curves for hills
                    cubicTo(
                        size.width * (0.2f + i * 0.05f), startY - (50 * i).dp.toPx(),
                        size.width * (0.6f - i * 0.05f), startY + (30 * i).dp.toPx(),
                        size.width, startY - (20 * i).dp.toPx()
                    )
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(path, color = animatedTopColor.copy(alpha = 0.03f * i))
            }

            // Draw sun or moon based on time
            val celestialColor = if (h in 5..19) Color(0xFFFFD166) else Color(0xFFF0FAF8)
            val celestialY = when (h) {
                in 5..8 -> size.height * (0.4f - ((h - 5) / 3f) * 0.2f) // Rising
                in 9..16 -> size.height * 0.15f // High
                in 17..19 -> size.height * (0.15f + ((h - 17) / 2f) * 0.45f) // Setting
                else -> size.height * 0.15f // Moon high
            }

            drawCircle(
                color = celestialColor.copy(alpha = 0.35f),
                radius = 45.dp.toPx(),
                center = Offset(size.width * 0.85f, celestialY)
            )

            // Nature details
            if (h !in 5..19) {
                // Stars
                for (i in 0..40) {
                    val random = Random(i.toLong())
                    drawCircle(
                        Color.White.copy(alpha = 0.6f),
                        radius = 1.dp.toPx(),
                        center = Offset(random.nextFloat() * size.width, random.nextFloat() * size.height * 0.6f)
                    )
                }
            } else {
                // Clouds
                for (i in 0..5) {
                    val random = Random(i.toLong())
                    val cloudX = random.nextFloat() * size.width
                    val cloudY = random.nextFloat() * size.height * 0.3f
                    drawCircle(Color.White.copy(alpha = 0.25f), radius = 30.dp.toPx(), center = Offset(cloudX, cloudY))
                    drawCircle(Color.White.copy(alpha = 0.25f), radius = 20.dp.toPx(), center = Offset(cloudX + 20.dp.toPx(), cloudY + 10.dp.toPx()))
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Nature",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(32.dp))

            var searchText by remember { mutableStateOf("") }
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Explore the stream...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = CircleShape,
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.8f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.5f)
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onSearch(searchText) }),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "\"$quote\"",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(shortcuts) { index, pair ->
                    ShortcutItem(pair.first, onRemove = {
                        shortcuts = shortcuts.toMutableList().apply { removeAt(index) }
                    }) { onShortcutClick(pair.second) }
                }
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.White.copy(alpha = 0.4f), CircleShape)
                                .clickable { showAddDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.Gray)
                        }
                    }
                }
            }
        }

        Text(
            text = "22°C • Clear Sky",
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        if (showAddDialog) {
            var newName by remember { mutableStateOf("") }
            var newUrl by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("New Quick Dial") },
                text = {
                    Column {
                        TextField(value = newName, onValueChange = { newName = it }, label = { Text("Name") })
                        TextField(value = newUrl, onValueChange = { newUrl = it }, label = { Text("URL") })
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (newName.isNotEmpty() && newUrl.isNotEmpty()) {
                            shortcuts = shortcuts + (newName to newUrl)
                            showAddDialog = false
                        }
                    }) { Text("Add") }
                }
            )
        }
    }
}

@Composable
fun ShortcutItem(name: String, onRemove: () -> Unit, onClick: () -> Unit) {
    Box {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable(onClick = onClick)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.White.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(name.take(1), style = MaterialTheme.typography.headlineMedium, color = Color(0xFF2A9D8F))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(20.dp).align(Alignment.TopEnd).background(Color.White, CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp))
        }
    }
}
