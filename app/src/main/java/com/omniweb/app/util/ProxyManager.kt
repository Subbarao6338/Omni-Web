package com.omniweb.app.util

import android.content.Context
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import java.util.concurrent.Executor

object ProxyManager {
    fun setProxy(host: String, port: Int, executor: Executor = Executor { it.run() }) {
        if (androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.PROXY_OVERRIDE)) {
            val proxyConfig = ProxyConfig.Builder()
                .addProxyRule("$host:$port")
                .addDirect()
                .build()
            
            ProxyController.getInstance().setProxyOverride(proxyConfig, executor, {
                // Proxy set successfully
            })
        }
    }

    fun clearProxy(executor: Executor = Executor { it.run() }) {
        if (androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.PROXY_OVERRIDE)) {
            ProxyController.getInstance().clearProxyOverride(executor, {
                // Proxy cleared successfully
            })
        }
    }
}
