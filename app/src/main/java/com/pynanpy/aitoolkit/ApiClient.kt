package com.pynanpy.aitoolkit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject

import java.util.concurrent.atomic.AtomicReference

object ApiClient {

    private val client = OkHttpClient.Builder()
        .build()

    private val currentCall =
        AtomicReference<Call?>(null)

    fun cancelCurrentRequest() {
        currentCall.getAndSet(null)?.cancel()
    }

    suspend fun sendMessageStream(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        onChunk: suspend (String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {

        try {

            val url =
                baseUrl.trimEnd('/') + "/chat/completions"

            val json =
                JSONObject()

            json.put(
                "model",
                model
            )

            json.put(
                "stream",
                true
            )

            val jsonMessages =
                JSONArray()

            for (message in messages) {

                val item =
                    JSONObject()

                item.put(
                    "role",
                    if (message.isUser) {
                        "user"
                    } else {
                        "assistant"
                    }
                )

                item.put(
                    "content",
                    message.text
                )

                jsonMessages.put(
                    item
                )
            }

            json.put(
                "messages",
                jsonMessages
            )

            val body =
                json.toString()
                    .toRequestBody(
                        "application/json".toMediaType()
                    )

            val request =
                Request.Builder()
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

            val call =
                client.newCall(request)

            currentCall.set(call)

            call.execute().use { response ->

                currentCall.compareAndSet(
                    call,
                    null
                )

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

                val responseBody =
                    response.body
                        ?: return@withContext Result.failure(
                            Exception(
                                "服务器没有返回数据"
                            )
                        )

                val source =
                    responseBody.source()

                while (!source.exhausted()) {

                    val line =
                        source.readUtf8Line()
                            ?: continue

                    if (
                        !line.startsWith(
                            "data:"
                        )
                    ) {
                        continue
                    }

                    val data =
                        line.removePrefix(
                            "data:"
                        ).trim()

                    if (
                        data == "[DONE]"
                    ) {
                        break
                    }

                    if (
                        data.isEmpty()
                    ) {
                        continue
                    }

                    try {

                        val chunk =
                            JSONObject(data)

                        val choices =
                            chunk.optJSONArray(
                                "choices"
                            )

                        if (
                            choices == null ||
                            choices.length() == 0
                        ) {
                            continue
                        }

                        val choice =
                            choices.getJSONObject(0)

                        val delta =
                            choice.optJSONObject(
                                "delta"
                            )

                        val content =
                            delta?.optString(
                                "content",
                                ""
                            ) ?: ""

                        if (
                            content.isNotEmpty()
                        ) {

                            onChunk(
                                content
                            )
                        }

                    } catch (_: Exception) {
                        // 忽略无法解析的 SSE 数据
                    }
                }

                Result.success(Unit)
            }

        } catch (e: Exception) {

            currentCall.set(null)

            if (
                e is java.io.IOException &&
                e.message?.contains(
                    "canceled",
                    ignoreCase = true
                ) == true
            ) {

                return@withContext Result.success(
                    Unit
                )
            }

            Result.failure(e)
        }
    }
}