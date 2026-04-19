package com.morninggrace.orchestrator.news

import com.morninggrace.core.model.NewsHeadline
import com.morninggrace.core.repository.NewsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

class RssNewsRepository @Inject constructor(
    private val client: OkHttpClient
) : NewsRepository {

    // BBC Chinese simplified — free, no auth, stable
    private val feedUrl = "https://feeds.bbci.co.uk/zhongwen/simp/rss.xml"

    override suspend fun getTopHeadlines(count: Int): List<NewsHeadline> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = client.newCall(
                    Request.Builder().url(feedUrl).header("User-Agent", "Mozilla/5.0").build()
                ).execute().use { response ->
                    if (!response.isSuccessful) return@runCatching emptyList()
                    response.body?.string()
                } ?: return@runCatching emptyList()

                Regex("<title>(?:<!\\[CDATA\\[)?([^<\\]]{5,})(?:]]>)?</title>")
                    .findAll(body)
                    .mapNotNull { it.groupValues[1].trim().ifBlank { null } }
                    .filter { it != "BBC Chinese" && !it.startsWith("BBC") }
                    .take(count)
                    .map { NewsHeadline(it) }
                    .toList()
            }.getOrElse { emptyList() }
        }
}
