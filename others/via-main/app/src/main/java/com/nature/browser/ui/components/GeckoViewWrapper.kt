package com.nature.browser.ui.components

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.URLUtil
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import android.graphics.Bitmap
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import com.nature.browser.TabModel
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.WebResponse
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nature.browser.BrowserViewModel

@Composable
fun GeckoViewWrapper(
    tab: TabModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: BrowserViewModel = viewModel()
    var geckoViewRef by remember { mutableStateOf<GeckoView?>(null) }
    var previewUrl by remember { mutableStateOf<String?>(null) }
    var previewTitle by remember { mutableStateOf<String?>(null) }

    DisposableEffect(tab) {
        viewModel.engine.onContextMenuHandler = { url, title ->
            previewUrl = url
            previewTitle = title
        }
        onDispose {
            viewModel.engine.onContextMenuHandler = null
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                GeckoView(ctx).apply {
                    setSession(tab.session)
                    geckoViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                geckoViewRef = view
                if (view.session != tab.session) {
                    view.setSession(tab.session)
                }
            }
        )

        val progress by tab.progress.collectAsState()
        if (progress > 0 && progress < 100) {
            RippleProgressBar(progress = progress / 100f)
        }

        previewUrl?.let { url ->
            Popup(onDismissRequest = {
                previewUrl = null
                previewTitle = null
            }) {
                Card(
                    modifier = Modifier
                        .padding(32.dp)
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FAF8)),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(previewTitle ?: "Stream Preview", style = MaterialTheme.typography.titleMedium, color = Color(0xFF2A9D8F))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(url, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = {
                                previewUrl = null
                                previewTitle = null
                            }) {
                                Text("Quiet the Ripples", color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    tab.session.loadUri(url)
                                    previewUrl = null
                                    previewTitle = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A9D8F))
                            ) {
                                Text("Flow to this Stream")
                            }
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(tab) {
        tab.onPageStop = {
            geckoViewRef?.let { view ->
                if (view.width > 0 && view.height > 0) {
                    try {
                        val bitmap = Bitmap.createBitmap(view.width / 2, view.height / 2, Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(bitmap)
                        canvas.scale(0.5f, 0.5f)
                        view.draw(canvas)
                        tab.thumbnail.value = bitmap
                    } catch (e: Exception) {}
                }
            }
        }

        onDispose {
            tab.onPageStop = null
        }
    }
}

@Composable
fun RippleProgressBar(progress: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "ripple")
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleScale"
    )
    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleAlpha"
    )

    Canvas(modifier = Modifier.fillMaxWidth().height(4.dp)) {
        val width = size.width
        val height = size.height

        drawRect(
            color = Color(0xFF2A9D8F).copy(alpha = 0.3f),
            size = size
        )
        drawRect(
            color = Color(0xFF2A9D8F),
            size = size.copy(width = width * progress)
        )

        if (progress > 0) {
            val rippleX = width * progress
            drawCircle(
                color = Color(0xFF57CC99).copy(alpha = rippleAlpha),
                radius = 10.dp.toPx() * rippleScale,
                center = androidx.compose.ui.geometry.Offset(rippleX, height / 2),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
