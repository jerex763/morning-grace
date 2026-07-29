package com.morninggrace.orchestrator

import com.morninggrace.orchestrator.news.RssNewsRepository
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Test

class RssNewsRepositoryTest {

    private val repository = RssNewsRepository(OkHttpClient())

    @Test
    fun `extracts all article paragraphs and ignores captions`() {
        val html = """
            <html>
            <!--正文start-->
            <div class="left_zw">
              <p>第一段新闻正文。</p>
              <div><img src="photo.jpg"></div>
              <div class="pictext">图片说明不应播报</div>
              <p>第二段包含<strong>重点</strong>。</p>
            </div>
            <!--正文end-->
            </html>
        """.trimIndent()

        assertEquals(
            "第一段新闻正文。\n第二段包含 重点 。",
            repository.extractArticleText(html)
        )
    }
}
