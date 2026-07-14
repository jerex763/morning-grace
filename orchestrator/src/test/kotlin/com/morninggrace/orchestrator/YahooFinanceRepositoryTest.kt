package com.morninggrace.orchestrator

import com.morninggrace.orchestrator.finance.YahooFinanceRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class YahooFinanceRepositoryTest {

    private val mockClient = mockk<OkHttpClient>()
    private val repo = YahooFinanceRepository(mockClient)

    private fun okCall(json: String): Call {
        val call = mockk<Call>(relaxed = true)
        val response = Response.Builder()
            .request(Request.Builder().url("https://query1.finance.yahoo.com").build())
            .protocol(Protocol.HTTP_1_1).code(200).message("OK")
            .body(json.toResponseBody()).build()
        every { call.enqueue(any()) } answers { firstArg<Callback>().onResponse(call, response) }
        return call
    }

    @Test
    fun `getMarketData returns data for all tickers`() = runTest {
        val json = """{"chart":{"result":[{"meta":{"regularMarketPrice":5000.0,"chartPreviousClose":4900.0}}],"error":null}}"""
        every { mockClient.newCall(any()) } answers { okCall(json) }

        val result = repo.getMarketData()

        assertEquals(4, result.size)
        assertEquals("标普五百", result[0].indexName)
        assertEquals(5000.0, result[0].price, 0.001)
    }

    @Test
    fun `getMarketData drops tickers with error payload`() = runTest {
        val json = """{"chart":{"result":null,"error":{"code":"Not Found"}}}"""
        every { mockClient.newCall(any()) } answers { okCall(json) }

        assertTrue(repo.getMarketData().isEmpty())
    }

    @Test
    fun `getMarketData drops tickers with empty result`() = runTest {
        val json = """{"chart":{"result":[],"error":null}}"""
        every { mockClient.newCall(any()) } answers { okCall(json) }

        assertTrue(repo.getMarketData().isEmpty())
    }

    @Test
    fun `getMarketData returns empty list on network failure`() = runTest {
        val call = mockk<Call>(relaxed = true)
        every { call.enqueue(any()) } answers { firstArg<Callback>().onFailure(call, IOException("timeout")) }
        every { mockClient.newCall(any()) } returns call

        assertTrue(repo.getMarketData().isEmpty())
    }

    @Test
    fun `toSpeechZh formats index as chinese numbers`() {
        val data = com.morninggrace.core.model.FinanceData("标普五百", 5234.0, 2.0)
        assertEquals("标普五百 五千两百三十四点，涨两个百分点", data.toSpeechZh())
    }

    @Test
    fun `toSpeechZh formats bitcoin in wan as chinese`() {
        val data = com.morninggrace.core.model.FinanceData("比特币", 630000.0, -1.5)
        assertEquals("比特币 六十三万美元，跌一点五个百分点", data.toSpeechZh())
    }
}
