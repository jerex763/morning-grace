package com.morninggrace.core.model

data class FinanceData(
    val indexName: String,
    val price: Double,
    val changePercent: Double
) {
    fun toSpeechZh(): String {
        val direction = if (changePercent >= 0) "涨" else "跌"
        val pct = formatPct(kotlin.math.abs(changePercent))
        val priceStr = if (indexName == "比特币") {
            "${toChineseNumber((price / 10000).toInt())}万美元"
        } else {
            "${toChineseNumber(price.toInt())}点"
        }
        return "$indexName $priceStr，$direction$pct"
    }

    private fun formatPct(value: Double): String {
        val i = value.toInt()
        val dec = ((value - i) * 10).toInt()
        return if (dec == 0) "${toChineseNumber(i)}个百分点"
        else "${toChineseNumber(i)}点${toChineseNumber(dec)}个百分点"
    }
}

private val DIGITS = arrayOf("零", "一", "二", "三", "四", "五", "六", "七", "八", "九")

fun toChineseNumber(n: Int): String {
    if (n == 0) return "零"
    if (n == 2) return "两"
    if (n < 0) return "负${toChineseNumber(-n)}"

    val yi  = n / 100_000_000
    val wan = (n % 100_000_000) / 10_000
    val rest = n % 10_000

    val sb = StringBuilder()
    if (yi > 0)  { sb.append(below10000(yi));  sb.append("亿") }
    if (wan > 0) {
        if (yi > 0 && wan < 1000) sb.append("零")
        sb.append(below10000(wan)); sb.append("万")
    }
    if (rest > 0) {
        if ((yi > 0 || wan > 0) && rest < 1000) sb.append("零")
        sb.append(below10000(rest))
    }
    return sb.toString()
}

private fun below10000(n: Int): String {
    val qian = n / 1000
    val bai  = (n % 1000) / 100
    val shi  = (n % 100) / 10
    val ge   = n % 10

    val sb = StringBuilder()
    if (qian > 0) { sb.append(if (qian == 2) "两" else DIGITS[qian]); sb.append("千") }
    if (bai  > 0) { sb.append(if (bai == 2) "两" else DIGITS[bai]); sb.append("百") }
    else if (qian > 0 && (shi > 0 || ge > 0)) sb.append("零")
    if (shi  > 0) { sb.append(DIGITS[shi]);  sb.append("十") }
    else if (bai > 0 && ge > 0) sb.append("零")
    if (ge   > 0) sb.append(DIGITS[ge])
    return sb.toString()
}
