package com.morninggrace.core.model

import java.time.LocalTime

object TimeGreeting {
    fun forTime(time: LocalTime): String = when (time.hour) {
        in 5..10 -> "早上好"
        in 11..13 -> "中午好"
        in 14..17 -> "下午好"
        in 18..22 -> "晚上好"
        else -> "夜深了"
    }
}
