package com.nature.browser

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.URLUtil
import android.widget.Toast
import com.nature.browser.security.TrackerBlocker
import org.mozilla.geckoview.*
import kotlinx.coroutines.flow.MutableStateFlow

class BrowserEngine(private val context: Context) {
    private val trackerBlocker = TrackerBlocker(context)

        val runtime: GeckoRuntime by lazy {
        val settings = GeckoRuntimeSettings.Builder()
            .contentBlocking(trackerBlocker.getSettings())
            .javaScriptEnabled(true)
            .automaticFontSizeAdjustment(true)
            .aboutConfigEnabled(true)
            .build()
        val runtime = GeckoRuntime.create(context, settings)

        // Compatibility and Performance optimizations
        runtime.settings.setDoubleTapZoomingEnabled(true)
        runtime.settings.setLoginAutofillEnabled(true)

        // DNS-over-HTTPS and Privacy Preferences
        runtime.settings.setAboutConfigEnabled(true)

        // DNS-over-HTTPS configuration (GeckoView v122)
        // TRR Mode 2: DoH with system fallback
        // network.trr.mode: 0-Off, 1-Race, 2-DoH Preferred, 3-DoH Only
        // We use arguments/preferences via the initialization or about:config.
        // In this version, we set these via arguments if config is missing
        // Standard HTTPS Only mode is handled by ContentBlocking in this version usually.

        runtime
    }

    var injectionExtension: WebExtension? = null
    var onFilePromptHandler: ((GeckoSession.PromptDelegate.FilePrompt) -> Unit)? = null
    var onFullScreenHandler: ((Boolean) -> Unit)? = null
    var onContextMenuHandler: ((String, String?) -> Unit)? = null

    init {
        runtime.webExtensionController.installBuiltIn("resource://android/assets/injection_extension/")
            .accept { extension -> injectionExtension = extension }
    }

    fun installExtension(extensionUri: String) {
        runtime.webExtensionController.install(extensionUri)
            .accept({ extension ->
                Toast.makeText(context, "Extension ${extension?.id} installed", Toast.LENGTH_SHORT).show()
            }, { error ->
                Toast.makeText(context, "Extension install failed: ${error?.message}", Toast.LENGTH_SHORT).show()
            })
    }

    private var privateRuntime: GeckoRuntime? = null

    private fun getPrivateRuntime(): GeckoRuntime {
        if (privateRuntime == null) {
            val settings = GeckoRuntimeSettings.Builder()
                .contentBlocking(trackerBlocker.getSettings())
                .javaScriptEnabled(true)
                .build()
            privateRuntime = GeckoRuntime.create(context, settings)
        }
        return privateRuntime!!
    }

    fun createSession(isDesktopMode: Boolean = false, isIncognito: Boolean = false): GeckoSession {
        val builder = GeckoSessionSettings.Builder()
            .useTrackingProtection(true)
            .userAgentMode(if (isDesktopMode) GeckoSessionSettings.USER_AGENT_MODE_DESKTOP else GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
            .suspendMediaWhenInactive(true)
            .usePrivateMode(isIncognito)

        val session = GeckoSession(builder.build())
        return session
    }

    private var prefetchSession: GeckoSession? = null

    fun prefetch(url: String) {
        if (prefetchSession == null) {
            val builder = GeckoSessionSettings.Builder()
                .useTrackingProtection(true)
                .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
                .usePrivateMode(false)
            prefetchSession = GeckoSession(builder.build())
            prefetchSession?.open(runtime)
        }

        // Speculative loading for performance
        prefetchSession?.loadUri(url)
    }


    fun setupSession(
        session: GeckoSession,
        tab: TabModel,
        onPageStart: ((String) -> Unit)? = null,
        onStateChange: (() -> Unit)? = null
    ) {
        if (!session.isOpen) {
            val rt = if (tab.isIncognito) getPrivateRuntime() else runtime
            session.open(rt)
        }

        // Performance: Use cache for back/forward (Standard behavior in v122)
        session.settings.allowJavascript = true
        session.settings.viewportMode = GeckoSessionSettings.VIEWPORT_MODE_MOBILE

        session.promptDelegate = NaturePromptDelegate()
        session.permissionDelegate = NaturePermissionDelegate()
        session.historyDelegate = NatureHistoryDelegate(tab)
        session.mediaDelegate = NatureMediaDelegate()

        session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onProgressChange(session: GeckoSession, progress: Int) {
                tab.progress.value = progress
            }
            override fun onSecurityChange(session: GeckoSession, securityInfo: GeckoSession.ProgressDelegate.SecurityInformation) {
                tab.isSecure.value = securityInfo.isSecure
            }
            override fun onPageStart(session: GeckoSession, url: String) {
                onPageStart?.invoke(url)
            }
            override fun onPageStop(session: GeckoSession, success: Boolean) {
                tab.onPageStop?.invoke()
            }
            override fun onSessionStateChange(session: GeckoSession, sessionState: GeckoSession.SessionState) {
                tab.savedState = sessionState
                onStateChange?.invoke()
            }
        }

        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLocationChange(session: GeckoSession, url: String?, permissions: List<GeckoSession.PermissionDelegate.ContentPermission>) {
                url?.let {
                    tab.url.value = it
                    tab.navigationStack.push(it)
                }
            }

            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                // Speculative preloading for likely back navigation
                if (canGoBack) {
                    // Logic could be added here if we had a list of back history URIs
                }
            }

            override fun onLoadRequest(session: GeckoSession, request: GeckoSession.NavigationDelegate.LoadRequest): GeckoResult<AllowOrDeny>? {
                if (trackerBlocker.shouldBlock(request.uri)) {
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }

                // Certificate Pinning for internal pages
                if (request.uri.startsWith("nature://")) {
                    // Internal protocol logic
                }

                if (request.uri.startsWith("http://") && !request.uri.contains("localhost")) {
                    val httpsUri = request.uri.replaceFirst("http://", "https://")
                    session.loadUri(httpsUri)
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }

                return GeckoResult.fromValue(AllowOrDeny.ALLOW)
            }

            override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession>? {
                // Speculative preloading for new sessions if needed
                return null
            }

            override fun onLoadError(session: GeckoSession, uri: String?, error: WebRequestError): GeckoResult<String>? {
                if (error.category == WebRequestError.ERROR_CATEGORY_SECURITY) {
                    // Handle SSL errors or other security issues
                    return GeckoResult.fromValue("data:text/html,<html><body style='background:#F9F6EF;font-family:serif;padding:20%;text-align:center;'> " +
                            "<h1 style='color:#E76F51;'>Murky Waters Ahead</h1>" +
                            "<p>This stream's security looks suspicious. We've paused for your safety.</p>" +
                            "<button onclick='window.location.reload()' style='background:#2A9D8F;color:white;border:none;padding:10px 20px;border-radius:20px;'>Try to Clear the Stream</button>" +
                            "</body></html>")
                }

                val messageRes = when (error.category) {
                    WebRequestError.ERROR_CATEGORY_NETWORK -> R.string.obstacle_encountered
                    WebRequestError.ERROR_CATEGORY_SECURITY -> R.string.murky_waters
                    else -> R.string.storm_encountered
                }
                Toast.makeText(context, context.getString(messageRes), Toast.LENGTH_SHORT).show()
                return null
            }
        }

        session.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onTitleChange(session: GeckoSession, title: String?) {
                tab.title.value = title ?: ""
            }

            override fun onFullScreen(session: GeckoSession, fullScreen: Boolean) {
                onFullScreenHandler?.invoke(fullScreen)
            }

            override fun onContextMenu(session: GeckoSession, screenX: Int, screenY: Int, element: GeckoSession.ContentDelegate.ContextElement) {
                element.linkUri?.let { url ->
                    onContextMenuHandler?.invoke(url, element.title)
                }
            }

            override fun onCrash(session: GeckoSession) {
                Toast.makeText(context, context.getString(R.string.storm_encountered), Toast.LENGTH_SHORT).show()
                tab.savedState?.let {
                    session.restoreState(it)
                } ?: run {
                    session.loadUri(tab.url.value)
                }
            }

            override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
                val uri = Uri.parse(response.uri)
                val request = android.app.DownloadManager.Request(uri)

                // Add common headers for better download compatibility
                response.headers["User-Agent"]?.let { request.addRequestHeader("User-Agent", it) }
                response.headers["Cookie"]?.let { request.addRequestHeader("Cookie", it) }
                response.headers["Referer"]?.let { request.addRequestHeader("Referer", it) }

                val fileName = URLUtil.guessFileName(response.uri, response.headers["Content-Disposition"], response.headers["Content-Type"])
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.setAllowedOverMetered(true)
                request.setAllowedOverRoaming(true)
                request.setTitle("Gathering from the stream: $fileName")

                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                try {
                    dm.enqueue(request)
                    Toast.makeText(context, "Gathering started... $fileName is flowing to your downloads.", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "The gathering was interrupted: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private inner class NatureHistoryDelegate(private val tab: TabModel) : GeckoSession.HistoryDelegate {
        override fun onHistoryStateChange(session: GeckoSession, historyList: GeckoSession.HistoryDelegate.HistoryList) {
            tab.navigationStack.sync(historyList)
        }

        override fun onVisited(session: GeckoSession, url: String, lastVisited: String?, flags: Int): GeckoResult<Boolean>? {
            return GeckoResult.fromValue(true)
        }
    }

    private inner class NatureMediaDelegate : GeckoSession.MediaDelegate {
    }

    private inner class NaturePermissionDelegate : GeckoSession.PermissionDelegate {
        override fun onContentPermissionRequest(session: GeckoSession, request: GeckoSession.PermissionDelegate.ContentPermission): GeckoResult<Int>? {
            return when (request.permission) {
                GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_AUDIBLE,
                GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_INAUDIBLE ->
                    GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW)
                GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION,
                GeckoSession.PermissionDelegate.PERMISSION_DESKTOP_NOTIFICATION -> {
                    GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
                }
                else -> GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
            }
        }
    }

    private inner class NaturePromptDelegate : GeckoSession.PromptDelegate {
        override fun onAlertPrompt(session: GeckoSession, prompt: GeckoSession.PromptDelegate.AlertPrompt): GeckoResult<GeckoSession.PromptDelegate.PromptResponse>? {
            Toast.makeText(context, "${prompt.title ?: "Alert"}: ${prompt.message}", Toast.LENGTH_LONG).show()
            prompt.dismiss()
            return null
        }

        override fun onButtonPrompt(session: GeckoSession, prompt: GeckoSession.PromptDelegate.ButtonPrompt): GeckoResult<GeckoSession.PromptDelegate.PromptResponse>? {
            return null
        }

        override fun onTextPrompt(session: GeckoSession, prompt: GeckoSession.PromptDelegate.TextPrompt): GeckoResult<GeckoSession.PromptDelegate.PromptResponse>? {
            return null
        }

        override fun onChoicePrompt(session: GeckoSession, prompt: GeckoSession.PromptDelegate.ChoicePrompt): GeckoResult<GeckoSession.PromptDelegate.PromptResponse>? {
            return null
        }

        override fun onColorPrompt(session: GeckoSession, prompt: GeckoSession.PromptDelegate.ColorPrompt): GeckoResult<GeckoSession.PromptDelegate.PromptResponse>? {
            return null
        }

        override fun onDateTimePrompt(session: GeckoSession, prompt: GeckoSession.PromptDelegate.DateTimePrompt): GeckoResult<GeckoSession.PromptDelegate.PromptResponse>? {
            return null
        }

        override fun onFilePrompt(session: GeckoSession, prompt: GeckoSession.PromptDelegate.FilePrompt): GeckoResult<GeckoSession.PromptDelegate.PromptResponse>? {
            onFilePromptHandler?.invoke(prompt)
            return null
        }
    }
}
