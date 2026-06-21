package com.omniweb.app.engine

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebExtension
import com.omniweb.app.data.TabInfo
import com.omniweb.app.util.AdBlockManager

class GeckoEngine(context: Context) {
    val runtime: GeckoRuntime by lazy { GeckoRuntime.create(context) }
}

@Composable
fun GeckoViewContainer(
    tab: TabInfo,
    modifier: Modifier = Modifier,
    onTitleReceived: (String) -> Unit = {},
    onProgressChanged: (Float) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val geckoEngine = remember { GeckoEngine(context.applicationContext) }

    val session = remember(tab.id) {
        GeckoSession().apply {
            open(geckoEngine.runtime)
            navigationDelegate = object : GeckoSession.NavigationDelegate {
                override fun onLocationChange(session: GeckoSession, url: String?, permissions: List<GeckoSession.PermissionDelegate.ContentPermission>) {
                    url?.let { tab.url = it }
                }
            }
            progressDelegate = object : GeckoSession.ProgressDelegate {
                override fun onProgressChange(session: GeckoSession, progress: Int) {
                    onProgressChanged(progress / 100f)
                }
            }
            contentDelegate = object : GeckoSession.ContentDelegate {
                override fun onTitleChange(session: GeckoSession, title: String?) {
                    title?.let { onTitleReceived(it) }
                }
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            GeckoView(ctx).apply {
                AdBlockManager.init(ctx.applicationContext)
                setSession(session)
                if (tab.url.isNotBlank() && tab.url != "about:home") {
                    session.loadUri(tab.url)
                }
            }
        },
        modifier = modifier
    )

    DisposableEffect(tab.id) {
        onDispose {
            session.close()
        }
    }
}
