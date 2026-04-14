package com.morninggrace.orchestrator

/**
 * All content gathered during PREPARING phase, ready for TTS playback.
 * [weather] and [finance] are stubs in Plan 3; replaced in Plan 4.
 */
data class BroadcastContent(
    val greeting: String,
    val bibleZh: String,       // CUV text of today's first passage
    val bibleEn: String,       // ESV/WEB text of today's first passage
    val weather: String,       // stub: "天气功能即将推出"
    val finance: String        // stub: "财经功能即将推出"
)
