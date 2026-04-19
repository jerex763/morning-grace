package com.morninggrace.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceDataTest {

    // ── toChineseNumber ──────────────────────────────────────────────────────

    @Test fun `zero returns 零`() = assertEquals("零", toChineseNumber(0))
    @Test fun `two returns 两`() = assertEquals("两", toChineseNumber(2))
    @Test fun `negative delegates`() = assertEquals("负三", toChineseNumber(-3))

    @Test fun `single digits`() {
        assertEquals("一", toChineseNumber(1))
        assertEquals("九", toChineseNumber(9))
    }

    @Test fun `tens`() {
        assertEquals("十", toChineseNumber(10))
        assertEquals("十一", toChineseNumber(11))
        assertEquals("十九", toChineseNumber(19))
    }

    @Test fun `hundreds`() {
        assertEquals("一百", toChineseNumber(100))
        assertEquals("两百", toChineseNumber(200))
        assertEquals("一百零一", toChineseNumber(101))
        assertEquals("一百一十", toChineseNumber(110))
        assertEquals("一百一十一", toChineseNumber(111))
    }

    @Test fun `thousands`() {
        assertEquals("一千", toChineseNumber(1000))
        assertEquals("两千", toChineseNumber(2000))
        assertEquals("一千零一", toChineseNumber(1001))
        assertEquals("一千一百", toChineseNumber(1100))
        assertEquals("五千", toChineseNumber(5000))
    }

    @Test fun `wan range`() {
        assertEquals("一万", toChineseNumber(10_000))
        assertEquals("两万", toChineseNumber(20_000))
        assertEquals("七万一千两百六十", toChineseNumber(71_260))
    }

    @Test fun `large stock index values`() {
        // S&P ~5000, NASDAQ ~16000
        assertEquals("五千", toChineseNumber(5000))
        assertEquals("一万六千", toChineseNumber(16_000))
    }

    @Test fun `bitcoin price divisor`() {
        // BTC ~$60,000 → price/10000 = 6 → 六
        assertEquals("六", toChineseNumber(6))
    }

    // ── toSpeechZh ───────────────────────────────────────────────────────────

    @Test fun `positive change uses 涨`() {
        val speech = FinanceData("标普500", 5000.0, 0.5).toSpeechZh()
        assertTrue("expected 涨 in: $speech", speech.contains("涨"))
    }

    @Test fun `negative change uses 跌`() {
        val speech = FinanceData("标普500", 5000.0, -1.2).toSpeechZh()
        assertTrue("expected 跌 in: $speech", speech.contains("跌"))
    }

    @Test fun `zero change uses 涨`() {
        val speech = FinanceData("纳斯达克", 16_000.0, 0.0).toSpeechZh()
        assertTrue("expected 涨 for 0%: $speech", speech.contains("涨"))
    }

    @Test fun `bitcoin shows 万美元`() {
        val speech = FinanceData("比特币", 60_000.0, 1.0).toSpeechZh()
        assertTrue("expected 万美元 in: $speech", speech.contains("万美元"))
    }

    @Test fun `non-bitcoin shows 点`() {
        val speech = FinanceData("上证指数", 3200.0, -0.5).toSpeechZh()
        assertTrue("expected 点 in: $speech", speech.contains("点"))
    }

    @Test fun `index name appears in speech`() {
        val speech = FinanceData("标普500", 5000.0, 0.5).toSpeechZh()
        assertTrue(speech.startsWith("标普500"))
    }
}
