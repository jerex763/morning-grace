package com.morninggrace.orchestrator

import com.morninggrace.orchestrator.news.RssNewsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RssNewsRepositoryTest {

    private val mockClient = mockk<OkHttpClient>()
    private val repo = RssNewsRepository(mockClient)

    private fun stubResponse(body: String) {
        val mockCall = mockk<Call>()
        val response = Response.Builder()
            .request(Request.Builder().url("https://feeds.bbci.co.uk").build())
            .protocol(Protocol.HTTP_1_1).code(200).message("OK")
            .body(body.toResponseBody()).build()
        every { mockClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } returns response
    }

    @Test
    fun `skips channel title and returns item titles only`() = runTest {
        stubResponse(
            """
            <rss><channel>
              <title>BBC Chinese</title>
              <image><title>BBC Chinese</title></image>
              <item><title><![CDATA[头条新闻一]]></title></item>
              <item><title>头条新闻二</title></item>
              <item><title><![CDATA[头条新闻三 [更新]]]></title></item>
            </channel></rss>
            """.trimIndent()
        )

        val headlines = repo.getTopHeadlines(3)

        assertEquals(3, headlines.size)
        assertEquals("头条新闻一", headlines[0].title)
        assertEquals("头条新闻二", headlines[1].title)
        assertTrue(headlines[2].title.startsWith("头条新闻三"))
    }

    @Test
    fun `respects count limit`() = runTest {
        stubResponse(
            """
            <rss><channel><title>BBC Chinese</title>
              <item><title>一</title></item>
              <item><title>二</title></item>
              <item><title>三</title></item>
            </channel></rss>
            """.trimIndent()
        )

        assertEquals(2, repo.getTopHeadlines(2).size)
    }

    @Test
    fun `returns empty list on network failure`() = runTest {
        val mockCall = mockk<Call>()
        every { mockClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } throws RuntimeException("timeout")

        assertTrue(repo.getTopHeadlines(3).isEmpty())
    }
}
