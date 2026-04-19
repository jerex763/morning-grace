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

    override suspend fun getTopHeadlines(count: Int): List<NewsHeadline> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = "https://feeds.reuters.com/reuters/CNbusinessNews"
                val body = client.newCall(
                    Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
                ).execute().use { response ->
                    if (!response.isSuccessful) return@runCatching emptyList()
                    response.body?.string()
                } ?: return@runCatching emptyList()

                Regex("<title><!\\[CDATA\\[(.+?)]]></title>|<title>([^<]+)</title>")
                    .findAll(body)
                    .mapNotNull { it.groupValues[1].ifBlank { it.groupValues[2] }.trim().ifBlank { null } }
                    .filter { it.isNotBlank() && it != "Reuters" }
                    .take(count)
                    .map { NewsHeadline(it) }
                    .toList()
            }.getOrElse { emptyList() }
        }
}
