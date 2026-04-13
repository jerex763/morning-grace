package com.morninggrace.core.model

data class AlarmConfig(
    val id: Int = 1,
    val hourOfDay: Int = 6,
    val minute: Int = 0,
    val enabled: Boolean = false,
    val language: Language = Language.ZH
)
