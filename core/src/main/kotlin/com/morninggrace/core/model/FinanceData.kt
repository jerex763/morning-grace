package com.morninggrace.core.model

data class FinanceData(
    val indexName: String,
    val price: Double,
    val changePercent: Double
) {
    fun toSpeechZh(): String {
        val direction = if (changePercent >= 0) "上涨" else "下跌"
        val pct = String.format("%.1f", kotlin.math.abs(changePercent))
        val priceStr = String.format("%.0f", price)
        return "${indexName}${priceStr}点，${direction}${pct}%"
    }
}
