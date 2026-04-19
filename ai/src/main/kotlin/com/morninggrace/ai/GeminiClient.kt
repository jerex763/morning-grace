package com.morninggrace.ai

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MorningGrace"
private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-lite:generateContent"
private const val PREFS = "ai_prefs"
const val KEY_GEMINI_API_KEY = "gemini_api_key"

@Singleton
class GeminiClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient
) : AiClient {

    override fun isConfigured(): Boolean = apiKey().isNotBlank()

    override suspend fun chat(systemPrompt: String, messages: List<ChatMessage>): String? =
        withContext(Dispatchers.IO) {
            val key = apiKey()
            if (key.isBlank()) return@withContext null

            runCatching {
                val body = buildRequestJson(systemPrompt, messages)
                val request = Request.Builder()
                    .url("$BASE_URL?key=$key")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w(TAG, "Gemini HTTP ${response.code}")
                        return@use null
                    }
                    parseResponse(response.body?.string() ?: return@use null)
                }
            }.onFailure { Log.e(TAG, "Gemini error: ${it.message}") }.getOrNull()
        }

    private fun buildRequestJson(systemPrompt: String, messages: List<ChatMessage>): JSONObject {
        val contents = JSONArray().apply {
            for (msg in messages) {
                put(JSONObject().apply {
                    put("role", msg.role)
                    put("parts", JSONArray().apply { put(JSONObject().put("text", msg.content)) })
                })
            }
        }
        return JSONObject().apply {
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().apply { put(JSONObject().put("text", systemPrompt)) })
            })
            put("contents", contents)
        }
    }

    private fun parseResponse(body: String): String? = runCatching {
        JSONObject(body)
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
            .trim()
    }.getOrNull()

    private fun apiKey(): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_GEMINI_API_KEY, "") ?: ""
}
