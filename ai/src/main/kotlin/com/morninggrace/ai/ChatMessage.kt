package com.morninggrace.ai

data class ChatMessage(
    val role: String,    // "user" or "model"
    val content: String
)
