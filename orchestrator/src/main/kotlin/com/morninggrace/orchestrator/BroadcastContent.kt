package com.morninggrace.orchestrator

data class BroadcastContent(
    val greeting: String,
    val passageName: String,   // e.g. "马太福音第一章", blank when Bible is skipped
    val weather: String,
    val bibleZh: String,
    val bibleEn: String,
    val marketSummary: String,
    val newsSummary: String
)
