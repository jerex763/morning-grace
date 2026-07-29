package com.morninggrace.core.model

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeGreetingTest {

    @Test
    fun `uses the expected greeting at every time boundary`() {
        val cases = mapOf(
            LocalTime.of(4, 59) to "夜深了",
            LocalTime.of(5, 0) to "早上好",
            LocalTime.of(10, 59) to "早上好",
            LocalTime.of(11, 0) to "中午好",
            LocalTime.of(13, 59) to "中午好",
            LocalTime.of(14, 0) to "下午好",
            LocalTime.of(17, 59) to "下午好",
            LocalTime.of(18, 0) to "晚上好",
            LocalTime.of(22, 59) to "晚上好",
            LocalTime.of(23, 0) to "夜深了"
        )

        cases.forEach { (time, expected) ->
            assertEquals(expected, TimeGreeting.forTime(time))
        }
    }
}
