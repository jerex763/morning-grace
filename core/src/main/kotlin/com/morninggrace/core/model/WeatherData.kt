package com.morninggrace.core.model

data class WeatherData(
    val temperatureCelsius: Double,
    val weatherCode: Int
) {
    fun toSpeechZh(): String {
        val desc = when (weatherCode) {
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
        }
        val temp = temperatureCelsius.toInt()
        return "今天天气${desc}，${temp}摄氏度"
    }
}
