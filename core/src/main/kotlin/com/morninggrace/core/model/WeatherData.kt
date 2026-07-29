package com.morninggrace.core.model

data class WeatherData(
    val temperatureCelsius: Double,
    val weatherCode: Int,
    val humidity: Int,
    val windSpeedKmh: Double,
    val uvIndex: Double? = null,
    val locationName: String = "",
    val descriptionZh: String = ""
) {
    fun toSpeechZh(): String {
        val desc = descriptionZh.ifBlank { when (weatherCode) {
            0 -> "晴天"
            1, 2 -> "少云"
            3 -> "多云"
            45, 48 -> "雾"
            51, 53, 55 -> "毛毛雨"
            61, 63, 65 -> "下雨"
            71, 73, 75 -> "下雪"
            80, 81, 82 -> "阵雨"
            95 -> "雷阵雨"
            else -> "天气不明"
        } }
        val wind = when {
            windSpeedKmh < 10 -> "微风"
            windSpeedKmh < 30 -> "轻风"
            windSpeedKmh < 50 -> "中风"
            else -> "大风"
        }
        val uvSpeech = uvIndex?.let { value ->
            val level = when {
                value < 3 -> "低"
                value < 6 -> "中等"
                value < 8 -> "高"
                else -> "极高"
            }
            "，紫外线$level"
        }
        val place = locationName.trim().takeIf { it.isNotEmpty() }?.let { "${it}今天" } ?: "今天"
        return "${place}天气${desc}，${temperatureCelsius.toInt()}度，湿度${humidity}%，$wind${uvSpeech.orEmpty()}"
    }
}
