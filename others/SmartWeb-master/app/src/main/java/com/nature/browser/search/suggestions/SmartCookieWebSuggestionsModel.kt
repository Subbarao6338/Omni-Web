package com.nature.browser.search.suggestions

import android.app.Application
import com.nature.browser.R
import com.nature.browser.constant.UTF8
import com.nature.browser.database.SearchSuggestion
import com.nature.browser.extensions.map
import com.nature.browser.extensions.preferredLocale
import com.nature.browser.log.Logger
import io.reactivex.Single
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Search suggestions provider for Google search engine.
 */
class NatureBrowserSuggestionsModel(
    okHttpClient: Single<OkHttpClient>,
    requestFactory: RequestFactory,
    application: Application,
    logger: Logger
) : BaseSuggestionsModel(okHttpClient, requestFactory, UTF8, application.preferredLocale, logger) {

    private val searchSubtitle = application.getString(R.string.suggestion)

    // https://smartcookieweb.com/autocomplete.php?query={query}
    override fun createQueryUrl(query: String, language: String): HttpUrl = HttpUrl.Builder()
            .scheme("https")
            .host("smartcookieweb.com")
            .encodedPath("/autocomplete.php")
            .addEncodedQueryParameter("query", query)
            .build()

    @Throws(Exception::class)
    override fun parseResults(responseBody: ResponseBody): List<SearchSuggestion> {
        return JSONObject(responseBody.string())
                .getJSONArray("results")
                .getJSONArray(0)
                .map { it as JSONArray }
                .map { it[0] as String }
                .map { SearchSuggestion("$searchSubtitle \"$it\"", it) }
    }

}
