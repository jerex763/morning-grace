package com.morninggrace.core.model

data class BroadcastConfig(
    val skipWeather:        Boolean = false,
    val skipBible:          Boolean = false,
    val includeEnglishBible: Boolean = false,
    val preferRecordedBible: Boolean = true,
    val skipNews:           Boolean = false,
    val newsFullArticles:   Boolean = false
)
