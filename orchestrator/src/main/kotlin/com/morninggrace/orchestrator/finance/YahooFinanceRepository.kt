package com.morninggrace.orchestrator.finance

import com.morninggrace.core.model.FinanceData
import com.morninggrace.core.net.await
import com.morninggrace.core.repository.FinanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

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
            try {
                val url = "https://query1.finance.yahoo.com/v8/finance/chart/$ticker?interval=1d&range=1d"
                val body = client.newCall(
                    Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
                ).await().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    response.body?.string()
                } ?: return@withContext null

                val (price, prevClose) = parsePrices(body) ?: return@withContext null
                val changePct = ((price - prevClose) / prevClose) * 100
                FinanceData(indexName = name, price = price, changePercent = changePct)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }

    private fun parsePrices(body: String): Pair<Double, Double>? {
        val chart = JSONObject(body).optJSONObject("chart") ?: return null
        if (!chart.isNull("error")) return null
        val result = chart.optJSONArray("result") ?: return null
        if (result.length() == 0) return null
        val meta = result.optJSONObject(0)?.optJSONObject("meta") ?: return null
        val price     = meta.optDouble("regularMarketPrice", Double.NaN)
        val prevClose = meta.optDouble("chartPreviousClose", Double.NaN)
        if (!price.isFinite() || !prevClose.isFinite() || price <= 0.0 || prevClose <= 0.0) return null
        return price to prevClose
    }
}
