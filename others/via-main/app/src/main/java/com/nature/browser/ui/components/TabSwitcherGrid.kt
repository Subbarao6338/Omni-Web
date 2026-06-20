package com.nature.browser.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nature.browser.BrowserViewModel
import com.nature.browser.TabModel

// Lily pad shape
val LilyPadShape = GenericShape { size, _ ->
    val radius = size.width / 2f
    addArc(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height), 0f, 330f)
    lineTo(radius, radius)
    close()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabSwitcherGrid(
    viewModel: BrowserViewModel,
    onTabSelected: (String) -> Unit,
    onClose: () -> Unit
) {
    val tabs by viewModel.tabs.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Floating Lily Pads", style = MaterialTheme.typography.titleLarge, color = Color(0xFF264653)) },
                actions = {
                    IconButton(onClick = {
                        viewModel.addTab()
                        onClose()
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "New Tab", tint = Color(0xFF2A9D8F))
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFF264653))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF0FAF8))
            )
        },
        containerColor = Color(0xFFF5F9F5) // Pond color
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(tabs) { tab ->
                LilyPadTab(
                    tab = tab,
                    isActive = tab.id == activeTabId,
                    onClick = {
                        onTabSelected(tab.id)
                        onClose()
                    },
                    onClose = { viewModel.closeTab(tab.id) }
                )
            }
        }
    }
}

@Composable
fun LilyPadTab(
    tab: TabModel,
    isActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    val title by tab.title.collectAsState()
    val url by tab.url.collectAsState()
    val haptic = LocalHapticFeedback.current

    var offsetX by remember { mutableStateOf(0f) }
    val infiniteTransition = rememberInfiniteTransition(label = "LilyPadAnims")
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    val transition = updateTransition(targetState = isActive, label = "LilyPadActive")
    val activeRippleScale by transition.animateFloat(
        label = "activeRippleScale",
        transitionSpec = { tween(1000, easing = EaseOutCirc) }
    ) { if (it) 1.2f else 1f }

    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseOutExpo),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleScale"
    )
    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleAlpha"
    )

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = expandIn(expandFrom = Alignment.Center, animationSpec = tween(600, easing = EaseOutExpo)) + fadeIn(),
        exit = shrinkOut(shrinkTowards = Alignment.Center) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .offset(x = offsetX.dp, y = floatAnim.dp)
                .aspectRatio(1f)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount
                        },
                        onDragEnd = {
                            if (kotlin.math.abs(offsetX) > 100) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onClose()
                            } else {
                                offsetX = 0f
                            }
                        },
                        onDragCancel = { offsetX = 0f }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color(0xFF57CC99).copy(alpha = rippleAlpha),
                    radius = (size.width / 2) * rippleScale,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize(0.85f)
                    .clip(LilyPadShape)
                    .background(if (isActive) Color(0xFF57CC99) else Color(0xFF2A9D8F).copy(alpha = 0.85f))
                    .clickable(onClick = onClick)
                    .padding(12.dp)
            ) {
                // Water ripple appear animation is handled by AnimatedVisibility outer wrapper
                val thumbnail by tab.thumbnail.collectAsState()

                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        alpha = 0.7f,
                        contentScale = ContentScale.Crop
                    )
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (title.isEmpty()) "New Tab" else title,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                            color = Color.White
                        )
                        IconButton(onClick = onClose, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = url,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White,
                            fontWeight = FontWeight.Light
                        )
                    }
                }
            }
        }
    }
}
