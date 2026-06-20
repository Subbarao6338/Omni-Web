package com.nature.browser

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class TabRepository(
    private val storage: BrowserStorage,
    private val engine: BrowserEngine
) {
    private val _tabs = MutableStateFlow<List<TabModel>>(emptyList())
    val tabs: StateFlow<List<TabModel>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    var onPageStartListener: ((String, String) -> Unit)? = null

    fun addTab(url: String, isIncognito: Boolean = false) {
        val session = engine.createSession(storage.isDesktopMode, isIncognito)
        val newTab = TabModel(session = session, isIncognito = isIncognito)
        engine.setupSession(
            session = newTab.session,
            tab = newTab,
            onPageStart = { startedUrl ->
                onPageStartListener?.invoke(newTab.id, startedUrl)
            },
            onStateChange = {
                if (!newTab.isIncognito) saveTabsState()
            }
        )
        newTab.session.loadUri(url)

        _tabs.value = _tabs.value + newTab
        if (!isIncognito) {
            _activeTabId.value = newTab.id
            saveTabsState()
        } else {
            _activeTabId.value = newTab.id
        }
    }

    fun closeTab(tabId: String) {
        val tabToClose = _tabs.value.find { it.id == tabId }
        tabToClose?.session?.close()
        _tabs.value = _tabs.value.filter { it.id != tabId }

        if (_activeTabId.value == tabId) {
            _activeTabId.value = _tabs.value.lastOrNull()?.id
        }

        if (_tabs.value.isEmpty()) {
            addTab(storage.homepage)
        }
        saveTabsState()
    }

    fun switchTab(tabId: String) {
        _activeTabId.value = tabId
        _tabs.value.find { it.id == tabId }?.lastActive = System.currentTimeMillis()
    }

    fun saveTabsState() {
        val array = JSONArray()
        _tabs.value.filter { !it.isIncognito }.forEach { tab ->
            val obj = JSONObject()
            obj.put("url", tab.url.value)
            obj.put("title", tab.title.value)
            tab.savedState?.let {
                obj.put("state", storage.serializeState(it))
            }
            obj.put("scrollX", tab.scrollX)
            obj.put("scrollY", tab.scrollY)
            obj.put("id", tab.id)
            array.put(obj)
        }
        storage.saveTabs(array.toString())
        _activeTabId.value?.let { storage.saveActiveTabId(it) }
    }

    fun restoreTabs() {
        val saved = storage.getSavedTabs()
        if (saved != null) {
            try {
                val array = JSONArray(saved)
                val restoredTabs = mutableListOf<TabModel>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val url = obj.getString("url")
                    val title = obj.optString("title", "")
                    val encodedState = obj.optString("state", "")
                    val scrollX = obj.optInt("scrollX", 0)
                    val scrollY = obj.optInt("scrollY", 0)
                    val id = obj.optString("id", java.util.UUID.randomUUID().toString())

                    val session = engine.createSession(storage.isDesktopMode)
                    val tab = TabModel(id = id, session = session)
                    tab.url.value = url
                    tab.title.value = title
                    tab.scrollX = scrollX
                    tab.scrollY = scrollY
                    if (encodedState.isNotEmpty()) {
                        tab.savedState = storage.deserializeState(encodedState)
                    }

                    engine.setupSession(
                        session = tab.session,
                        tab = tab,
                        onPageStart = { startedUrl ->
                            onPageStartListener?.invoke(tab.id, startedUrl)
                        },
                        onStateChange = {
                            if (!tab.isIncognito) saveTabsState()
                        }
                    )
                    restoredTabs.add(tab)
                }
                if (restoredTabs.isNotEmpty()) {
                    _tabs.value = restoredTabs
                    val activeTabId = storage.getActiveTabId()
                    val tabToActivate = restoredTabs.find { it.id == activeTabId } ?: restoredTabs.first()
                    _activeTabId.value = tabToActivate.id

                    // Restore the active tab session
                    if (tabToActivate.savedState != null) {
                        tabToActivate.session.restoreState(tabToActivate.savedState!!)
                        if (tabToActivate.scrollY > 0 || tabToActivate.scrollX > 0) {
                            tabToActivate.session.getPanZoomController().scrollTo(
                                org.mozilla.geckoview.ScreenLength.fromPixels(tabToActivate.scrollX.toDouble()),
                                org.mozilla.geckoview.ScreenLength.fromPixels(tabToActivate.scrollY.toDouble())
                            )
                        }
                    } else {
                        tabToActivate.session.loadUri(tabToActivate.url.value)
                    }
                    return
                }
            } catch (e: Exception) {
            }
        }
        addTab(storage.homepage)
    }
}
