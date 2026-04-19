package com.morninggrace.core.model

data class WeatherData(
    val temperatureCelsius: Double,
    val weatherCode: Int,
    val humidity: Int,
    val windSpeedKmh: Double,
    val uvIndex: Double
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
        val wind = when {
            windSpeedKmh < 10 -> "微风"
            windSpeedKmh < 30 -> "轻风"
            windSpeedKmh < 50 -> "中风"
            else -> "大风"
        }
        val uv = when {
            uvIndex < 3 -> "低"
            uvIndex < 6 -> "中"
            uvIndex < 8 -> "高"
            else -> "极高"
        }
        return "今天${desc}，${temperatureCelsius.toInt()}度，湿度${humidity}%，${wind}，紫外线${uv}"
    }
}
