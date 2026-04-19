package com.morninggrace.ai

interface AiClient {
    /** Returns the AI response, or null on error / not configured. */
    suspend fun chat(systemPrompt: String, messages: List<ChatMessage>): String?
    fun isConfigured(): Boolean
}
