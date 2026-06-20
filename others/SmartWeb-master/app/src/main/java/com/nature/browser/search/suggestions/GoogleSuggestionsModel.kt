package com.nature.browser.search.suggestions

import android.app.Application
import com.nature.browser.R
import com.nature.browser.constant.UTF8
import com.nature.browser.database.SearchSuggestion
import com.nature.browser.extensions.preferredLocale
import com.nature.browser.log.Logger
import io.reactivex.Single
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/**
 * Search suggestions provider for Google search engine.
 */
class GoogleSuggestionsModel(
    okHttpClient: Single<OkHttpClient>,
    requestFactory: RequestFactory,
    application: Application,
    logger: Logger
) : BaseSuggestionsModel(okHttpClient, requestFactory, UTF8, application.preferredLocale, logger) {

    private val searchSubtitle = application.getString(R.string.suggestion)
    // https://suggestqueries.google.com/complete/search?output=toolbar&hl={language}&q={query}
    override fun createQueryUrl(query: String, language: String): HttpUrl = HttpUrl.Builder()
        .scheme("https")
        .host("suggestqueries.google.com")
        .encodedPath("/complete/search")
        .addQueryParameter("output", "toolbar")
        //.addQueryParameter("hl", language)
        .addEncodedQueryParameter("q", query)
        .build()

    @Throws(Exception::class)
    override fun parseResults(responseBody: ResponseBody): List<SearchSuggestion> {
        parser.setInput(responseBody.byteStream(), encoding)
        val suggestions = mutableListOf<SearchSuggestion>()
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && "suggestion" == parser.name) {
                val suggestion = parser.getAttributeValue(null, "data")
                suggestions.add(SearchSuggestion("$searchSubtitle \"$suggestion\"", suggestion))
            }
            eventType = parser.next()
        }
        return suggestions
    }

    companion object {

        // Converting to a lambda results in pulling the newInstance call out of the lazy.
        @Suppress("ConvertLambdaToReference")
        private val parser by lazy {
            XmlPullParserFactory.newInstance().apply {
                isNamespaceAware = true
            }.newPullParser()
        }

        var encoding = "ISO-8859-1"

    }
}
