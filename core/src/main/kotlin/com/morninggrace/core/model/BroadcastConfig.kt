package com.morninggrace.core.model

data class BroadcastConfig(
    val skipWeather:        Boolean = false,
    val skipBible:          Boolean = false,
    val includeEnglishBible: Boolean = false,
    val skipFinance:        Boolean = false,
    val skipNews:           Boolean = false
)
