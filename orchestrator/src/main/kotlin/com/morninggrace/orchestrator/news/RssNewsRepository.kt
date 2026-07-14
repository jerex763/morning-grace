package com.morninggrace.orchestrator.news

import android.util.Xml
import com.morninggrace.core.model.NewsHeadline
import com.morninggrace.core.net.await
import com.morninggrace.core.repository.NewsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class RssNewsRepository @Inject constructor(
    private val client: OkHttpClient
) : NewsRepository {

    // BBC Chinese simplified — free, no auth, stable
    private val feedUrl = "https://feeds.bbci.co.uk/zhongwen/simp/rss.xml"

    override suspend fun getTopHeadlines(count: Int): List<NewsHeadline> =
        withContext(Dispatchers.IO) {
            try {
                val body = client.newCall(
                    Request.Builder().url(feedUrl).header("User-Agent", "Mozilla/5.0").build()
                ).await().use { response ->
                    if (!response.isSuccessful) return@withContext emptyList()
                    response.body?.string()
                } ?: return@withContext emptyList()
                parse(body, count)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            }
        }

    /** Reads only <item><title> text; XmlPullParser decodes entities and CDATA. */
    private fun parse(xml: String, count: Int): List<NewsHeadline> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))

        val titles = mutableListOf<String>()
        var insideItem = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT && titles.size < count) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "item" -> insideItem = true
                    "title" -> if (insideItem) {
                        val title = parser.nextText().trim()
                        if (title.isNotBlank()) titles += title
                    }
                }
                XmlPullParser.END_TAG -> if (parser.name == "item") insideItem = false
            }
            event = parser.next()
        }
        return titles.map { NewsHeadline(it) }
    }
}
