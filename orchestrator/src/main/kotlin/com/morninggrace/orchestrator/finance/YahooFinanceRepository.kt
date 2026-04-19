package com.morninggrace.orchestrator.finance

import com.morninggrace.core.model.FinanceData
import com.morninggrace.core.repository.FinanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject

class YahooFinanceRepository @Inject constructor(
    private val client: OkHttpClient
) : FinanceRepository {

    private val tickers = listOf(
        "^GSPC"     to "标普五百",
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

                val (price, prevClose) = parsePrices(body) ?: return@runCatching null
                val changePct = ((price - prevClose) / prevClose) * 100
                FinanceData(indexName = name, price = price, changePercent = changePct)
            }.getOrNull()
        }

    private fun parsePrices(body: String): Pair<Double, Double>? = runCatching {
        val meta = JSONObject(body)
            .getJSONObject("chart")
            .getJSONArray("result")
            .getJSONObject(0)
            .getJSONObject("meta")
        val price     = meta.optDouble("regularMarketPrice")
        val prevClose = meta.optDouble("chartPreviousClose")
        if (price.isNaN() || prevClose.isNaN() || prevClose == 0.0) return null
        price to prevClose
    }.getOrNull()
}
