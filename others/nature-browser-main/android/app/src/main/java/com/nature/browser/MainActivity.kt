package com.nature.browser

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

/**
 * Nature Browser MainActivity - Kotlin implementation using Mozilla GeckoView.
 * Provides a real Firefox-based browsing engine (Gecko) for the Nature Browser shell.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var geckoView: GeckoView
    private val geckoSession = GeckoSession()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup GeckoView
        geckoView = GeckoView(this)
        setContentView(geckoView)

        val runtime = GeckoRuntime.create(this)
        geckoSession.open(runtime)
        geckoView.setSession(geckoSession)

        // Load the Nature Browser Web Interface
        // Use the current environment APP_URL if possible, or fallback
        geckoSession.loadUrl("https://ais-dev-2tptid3poe7wicmq7wrl2j-274169280971.asia-southeast1.run.app")
    }

    override fun onDestroy() {
        super.onDestroy()
        geckoSession.close()
    }
}
