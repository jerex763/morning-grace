package com.morninggrace.core.model

data class NewsHeadline(
    val title: String,
    val summary: String = "",
    val fullContent: String = "",
    val articleUrl: String = ""
)
