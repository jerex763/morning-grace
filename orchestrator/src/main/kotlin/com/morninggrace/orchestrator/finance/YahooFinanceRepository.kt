package com.morninggrace.orchestrator.finance

import com.morninggrace.core.model.FinanceData
import com.morninggrace.core.repository.FinanceRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

class YahooFinanceRepository @Inject constructor(
    private val client: OkHttpClient,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : FinanceRepository {

    override suspend fun getSandP500(): FinanceData? =
        withContext(dispatcher) {
            runCatching {
                val url = "https://query1.finance.yahoo.com/v8/finance/chart/%5EGSPC?interval=1d&range=1d"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0")
                    .build()
                val body = client.newCall(request).execute()
                    .use { response ->
                        if (!response.isSuccessful) return@runCatching null
                        response.body?.string()
                    } ?: return@runCatching null
                val price = extractDouble(body, "regularMarketPrice") ?: return@runCatching null
                val prevClose = extractDouble(body, "chartPreviousClose") ?: return@runCatching null
                val changePct = ((price - prevClose) / prevClose) * 100
                FinanceData(indexName = "标普500", price = price, changePercent = changePct)
            }.getOrNull()
        }

    private fun extractDouble(json: String, key: String): Double? =
        Regex(""""$key"\s*:\s*([\d.]+)""").find(json)?.groupValues?.get(1)?.toDoubleOrNull()
}
