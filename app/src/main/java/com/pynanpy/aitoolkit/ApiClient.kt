package com.pynanpy.aitoolkit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject

object ApiClient {

    private val client = OkHttpClient.Builder()
        .build()

    suspend fun sendMessageStream(
        baseUrl: String,
        apiKey: String,
        model: String,
        message: String,
        onChunk: suspend (String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {

        try {

            val url =
                baseUrl.trimEnd('/') + "/chat/completions"

            val json = JSONObject()

            json.put("model", model)
            json.put("stream", true)

            val messages = org.json.JSONArray()

            val userMessage = JSONObject()
            userMessage.put("role", "user")
            userMessage.put("content", message)

            messages.put(userMessage)

            json.put("messages", messages)

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

                if (!response.isSuccessful) {

                    val error =
                        response.body?.string()
                            ?: "未知错误"

                    return@withContext Result.failure(
                        Exception(
                            "HTTP ${response.code}: $error"
                        )
                    )
                }

                val source =
                    response.body?.source()
                        ?: return@withContext Result.failure(
                            Exception("服务器没有返回数据")
                        )

                while (!source.exhausted()) {

                    val line =
                        source.readUtf8Line()
                            ?: continue

                    if (!line.startsWith("data:")) {
                        continue
                    }

                    val data =
                        line.removePrefix("data:")
                            .trim()

                    if (data == "[DONE]") {
                        break
                    }

                    try {

                        val jsonChunk =
                            JSONObject(data)

                        val choices =
                            jsonChunk.optJSONArray(
                                "choices"
                            )

                        if (choices == null ||
                            choices.length() == 0
                        ) {
                            continue
                        }

                        val choice =
                            choices.getJSONObject(0)

                        val delta =
                            choice.optJSONObject("delta")

                        val content =
                            delta?.optString(
                                "content",
                                ""
                            ) ?: ""

                        if (content.isNotEmpty()) {
                            onChunk(content)
                        }

                    } catch (_: Exception) {
                        // 忽略无法解析的 SSE 数据
                    }
                }

                Result.success(Unit)
            }

        } catch (e: Exception) {

            Result.failure(e)
        }
    }
}