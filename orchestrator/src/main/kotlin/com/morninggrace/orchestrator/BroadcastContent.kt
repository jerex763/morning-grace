package com.morninggrace.orchestrator

data class BroadcastContent(
    val greeting: String,
    val passageName: String,          // combined titles for the confirmation prompt, blank when skipped
    val weather: String,
    val passages: List<PassageReading>,
    val marketSummary: String,
    val newsSummary: String
)

data class PassageReading(
    val book: Int,
    val chapter: Int,
    val isWholeChapter: Boolean,
    val titleZh: String,
    val zh: String,
    val en: String
)
