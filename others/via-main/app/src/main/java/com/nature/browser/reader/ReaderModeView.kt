package com.nature.browser.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val LoraFont = FontFamily.Serif

@Composable
fun ReaderModeView(
    title: String,
    content: String,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F6EF)) // Parchment background
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Botanical vine border
            Canvas(modifier = Modifier.width(40.dp).fillMaxHeight()) {
                val path = Path().apply {
                    moveTo(size.width * 0.4f, 0f)
                    // More detailed vine curve using cubicTo for organic flow
                    for (i in 1..25) {
                        val y = size.height * (i / 25f)
                        val prevY = size.height * ((i - 1) / 25f)
                        val midY = (y + prevY) / 2f
                        val xOffset = if (i % 2 == 0) 12.dp.toPx() else -8.dp.toPx()

                        cubicTo(
                            size.width * 0.4f + xOffset, prevY + (y - prevY) * 0.3f,
                            size.width * 0.4f - xOffset * 0.5f, prevY + (y - prevY) * 0.7f,
                            size.width * 0.4f, y
                        )
                    }
                }
                drawPath(path, color = Color(0xFF2A9D8F), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))

                // Detailed leaves with organic placement and watercolor feel
                for (i in 1..45) {
                    val y = size.height * (i / 45f)
                    val isRight = i % 2 == 0
                    val leafLen = (6 + (i % 5)).dp.toPx()

                    val leafBase = androidx.compose.ui.geometry.Offset(size.width * 0.4f, y)
                    val leafPath = Path().apply {
                        moveTo(leafBase.x, leafBase.y)
                        val cp1 = androidx.compose.ui.geometry.Offset(
                            leafBase.x + (if (isRight) leafLen else -leafLen),
                            leafBase.y - leafLen
                        )
                        val tip = androidx.compose.ui.geometry.Offset(
                            leafBase.x + (if (isRight) leafLen * 2f else -leafLen * 2f),
                            leafBase.y - leafLen * 0.2f
                        )
                        val cp2 = androidx.compose.ui.geometry.Offset(
                            leafBase.x + (if (isRight) leafLen else -leafLen),
                            leafBase.y + leafLen * 0.5f
                        )
                        cubicTo(cp1.x, cp1.y, tip.x, tip.y, tip.x, tip.y)
                        cubicTo(tip.x, tip.y, cp2.x, cp2.y, leafBase.x, leafBase.y)
                    }
                    drawPath(leafPath, color = Color(0xFF57CC99).copy(alpha = 0.7f))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 8.dp, end = 24.dp, top = 24.dp, bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = LoraFont,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF264653)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close Reader Mode")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = LoraFont,
                        fontSize = 18.sp,
                        lineHeight = 32.sp,
                        color = Color(0xFF264653)
                    )
                )

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = "End of the stream.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF2A9D8F).copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
