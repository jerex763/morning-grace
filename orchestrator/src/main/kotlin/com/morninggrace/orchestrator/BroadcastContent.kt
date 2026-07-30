package com.morninggrace.orchestrator

data class BroadcastContent(
    val greeting: String,
    val passageName: String,
    val weather: String,
    val passages: List<PassageReading>,
    val news: List<NewsReading>
)

data class PassageReading(
    val book: Int,
    val chapter: Int,
    val isWholeChapter: Boolean,
    val titleZh: String,
    val zh: String,
    val en: String
)

data class NewsReading(
    val title: String,
    val content: String
)
