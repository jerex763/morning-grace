package com.morninggrace.orchestrator.finance

import com.morninggrace.core.model.FinanceData
import com.morninggrace.core.repository.FinanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

class YahooFinanceRepository @Inject constructor(
    private val client: OkHttpClient
) : FinanceRepository {

    private val tickers = listOf(
        "^GSPC"     to "标普500",
        "^IXIC"     to "纳斯达克",
        "000001.SS" to "上证",
        "BTC-USD"   to "比特币"
    )

    override suspend fun getMarketData(): List<FinanceData> = coroutineScope {
        tickers.map { (ticker, name) ->
            async { fetchTicker(ticker, name) }
        }.mapNotNull { it.await() }
    }

    private suspend fun fetchTicker(ticker: String, name: String): FinanceData? =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = "https://query1.finance.yahoo.com/v8/finance/chart/$ticker?interval=1d&range=1d"
                val body = client.newCall(
                    Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
                ).execute().use { response ->
                    if (!response.isSuccessful) return@runCatching null
                    response.body?.string()
                } ?: return@runCatching null

                val price = extractDouble(body, "regularMarketPrice") ?: return@runCatching null
                val prevClose = extractDouble(body, "chartPreviousClose") ?: return@runCatching null
                val changePct = ((price - prevClose) / prevClose) * 100
                FinanceData(indexName = name, price = price, changePercent = changePct)
            }.getOrNull()
        }

    private fun extractDouble(json: String, key: String): Double? =
        Regex(""""$key"\s*:\s*([\d.]+)""").find(json)?.groupValues?.get(1)?.toDoubleOrNull()
}
