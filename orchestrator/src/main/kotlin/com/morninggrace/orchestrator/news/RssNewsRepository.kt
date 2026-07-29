package com.morninggrace.orchestrator.news

import android.util.Xml
import com.morninggrace.core.model.NewsHeadline
import com.morninggrace.core.net.await
import com.morninggrace.core.repository.NewsRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

class RssNewsRepository @Inject constructor(
    private val client: OkHttpClient
) : NewsRepository {

    private val feedUrl = "https://www.chinanews.com.cn/rss/scroll-news.xml"

    override suspend fun getTopHeadlines(count: Int): List<NewsHeadline> =
        withContext(Dispatchers.IO) {
            try {
                val feed = get(feedUrl) ?: return@withContext emptyList()
                val items = parseFeed(feed, count)
                coroutineScope {
                    items.map { item ->
                        async {
                            val article = item.articleUrl
                                .takeIf { it.startsWith("https://www.chinanews.com.cn/") }
                                ?.let { get(it) }
                                ?.let(::extractArticleText)
                                .orEmpty()
                            item.copy(content = article.ifBlank { item.content })
                        }
                    }.awaitAll()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                emptyList()
            }
        }

    private suspend fun get(url: String): String? =
        client.newCall(
            Request.Builder()
                .url(url)
                .header("User-Agent", "MorningGrace/1.0 Android")
                .build()
        ).await().use { response ->
            if (!response.isSuccessful) null else response.body?.string()
        }

    private fun parseFeed(xml: String, count: Int): List<NewsHeadline> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))

        val items = mutableListOf<NewsHeadline>()
        var insideItem = false
        var title = ""
        var link = ""
        var description = ""
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT && items.size < count) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "item" -> {
                        insideItem = true
                        title = ""
                        link = ""
                        description = ""
                    }
                    "title" -> if (insideItem) title = parser.nextText().trim()
                    "link" -> if (insideItem) link = parser.nextText().trim()
                    "description" -> if (insideItem) {
                        description = cleanHtml(parser.nextText())
                    }
                }
                XmlPullParser.END_TAG -> if (parser.name == "item") {
                    if (title.isNotBlank()) {
                        items += NewsHeadline(
                            title = title,
                            content = description,
                            articleUrl = link
                        )
                    }
                    insideItem = false
                }
            }
            event = parser.next()
        }
        return items
    }

    internal fun extractArticleText(html: String): String {
        val body = Regex(
            "<!--正文start-->(.*?)<!--正文end-->",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        ).find(html)?.groupValues?.get(1).orEmpty()
        if (body.isBlank()) return ""

        return Regex(
            "<p(?:\\s[^>]*)?>(.*?)</p>",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        ).findAll(body)
            .map { cleanHtml(it.groupValues[1]) }
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }

    private fun cleanHtml(value: String): String =
        value
            .replace(Regex("<script.*?</script>", RegexOption.DOT_MATCHES_ALL), " ")
            .replace(Regex("<style.*?</style>", RegexOption.DOT_MATCHES_ALL), " ")
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&ldquo;", "“")
            .replace("&rdquo;", "”")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex("&#(\\d+);")) {
                it.groupValues[1].toIntOrNull()?.toChar()?.toString().orEmpty()
            }
            .replace(Regex("\\s+"), " ")
            .trim()
}
