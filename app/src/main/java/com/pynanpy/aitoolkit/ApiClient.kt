package com.pynanpy.aitoolkit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object ApiClient {

    private val client = OkHttpClient()

    suspend fun sendMessage(
        baseUrl: String,
        apiKey: String,
        model: String,
        message: String
    ): Result<String> = withContext(Dispatchers.IO) {

        try {

            val url =
                baseUrl.trimEnd('/') + "/chat/completions"

            val json = JSONObject()

            json.put("model", model)

            val messages = org.json.JSONArray()

            val userMessage = JSONObject()
            userMessage.put("role", "user")
            userMessage.put("content", message)

            messages.put(userMessage)

            json.put("messages", messages)

            json.put("stream", false)

            val body = json
                .toString()
                .toRequestBody(
                    "application/json".toMediaType()
                )

            val request = Request.Builder()
                .url(url)
                .post(body)
                .addHeader(
                    "Authorization",
                    "Bearer $apiKey"
                )
                .addHeader(
                    "Content-Type",
                    "application/json"
                )
                .build()

            client.newCall(request).execute().use { response ->

                val responseBody =
                    response.body?.string()
                        ?: ""

                if (!response.isSuccessful) {

                    return@withContext Result.failure(
                        Exception(
                            "HTTP ${response.code}: $responseBody"
                        )
                    )
                }

                val responseJson =
                    JSONObject(responseBody)

                val choices =
                    responseJson.getJSONArray("choices")

                val firstChoice =
                    choices.getJSONObject(0)

                val messageObject =
                    firstChoice.getJSONObject("message")

                val content =
                    messageObject.getString("content")

                Result.success(content)
            }

        } catch (e: Exception) {

            Result.failure(e)
        }
    }
}