package com.morninggrace.core.model

data class FinanceData(
    val indexName: String,
    val price: Double,
    val changePercent: Double
) {
    fun toSpeechZh(): String {
        val direction = if (changePercent >= 0) "涨" else "跌"
        val pct = String.format("%.1f", kotlin.math.abs(changePercent))
        val priceStr = if (indexName == "比特币") {
            val wan = (price / 10000).toInt()
            "${wan}万美元"
        } else {
            String.format("%.0f", price) + "点"
        }
        return "$indexName $priceStr，$direction${pct}%"
    }
}
