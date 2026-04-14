package com.morninggrace.orchestrator

import com.morninggrace.orchestrator.finance.YahooFinanceRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class YahooFinanceRepositoryTest {

    private val mockClient = mockk<OkHttpClient>()
    private val repo = YahooFinanceRepository(mockClient, UnconfinedTestDispatcher())

    @Test
    fun `returns FinanceData on valid response`() = runTest {
        val json = """{"chart":{"result":[{"meta":{"regularMarketPrice":5000.0,"chartPreviousClose":4900.0}}]}}"""
        val mockCall = mockk<Call>()
        val response = Response.Builder()
            .request(Request.Builder().url("https://query1.finance.yahoo.com").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(json.toResponseBody())
            .build()
        every { mockClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } returns response

        val result = repo.getSandP500()

        assertEquals("标普500", result?.indexName)
        assertEquals(5000.0, result?.price!!, 0.001)
        val expectedPct = ((5000.0 - 4900.0) / 4900.0) * 100
        assertEquals(expectedPct, result.changePercent, 0.001)
    }

    @Test
    fun `returns null on network failure`() = runTest {
        val mockCall = mockk<Call>()
        every { mockClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } throws RuntimeException("timeout")

        val result = repo.getSandP500()

        assertNull(result)
    }

    @Test
    fun `toSpeechZh returns correct string for positive change`() {
        val data = com.morninggrace.core.model.FinanceData(
            indexName = "标普500",
            price = 5000.0,
            changePercent = 2.04
        )
        assertEquals("标普5005000点，上涨2.0%", data.toSpeechZh())
    }
}
