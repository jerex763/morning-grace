package com.morninggrace.bible.model

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "bible_verses",
    primaryKeys = ["book", "chapter", "verse", "lang"],
    indices = [Index("book", "chapter", "lang")]
)
data class BibleVerse(
    val book: Int,
    val chapter: Int,
    val verse: Int,
    val lang: String,   // "zh" or "en"
    val text: String
)
