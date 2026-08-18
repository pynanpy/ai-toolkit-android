package com.pynanpy.aitoolkit

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ChatRepository {

    private const val PREFS_NAME = "ai_toolkit_chats"
    private const val KEY_CONVERSATIONS = "conversations"

    private fun prefs(context: Context) =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    fun saveConversations(
        context: Context,
        conversations: List<ChatConversation>
    ) {

        val array = JSONArray()

        conversations.forEach { conversation ->

            val conversationJson = JSONObject()

            conversationJson.put(
                "id",
                conversation.id
            )

            conversationJson.put(
                "title",
                conversation.title
            )

            conversationJson.put(
                "createdAt",
                conversation.createdAt
            )

            conversationJson.put(
                "updatedAt",
                conversation.updatedAt
            )

            val messages = JSONArray()

            conversation.messages.forEach { message ->

                val messageJson = JSONObject()

                messageJson.put(
                    "id",
                    message.id
                )

                messageJson.put(
                    "text",
                    message.text
                )

                messageJson.put(
                    "isUser",
                    message.isUser
                )

                messageJson.put(
                    "timestamp",
                    message.timestamp
                )

                messages.put(messageJson)
            }

            conversationJson.put(
                "messages",
                messages
            )

            array.put(conversationJson)
        }

        prefs(context)
            .edit()
            .putString(
                KEY_CONVERSATIONS,
                array.toString()
            )
            .apply()
    }

    fun loadConversations(
        context: Context
    ): List<ChatConversation> {

        val raw =
            prefs(context)
                .getString(
                    KEY_CONVERSATIONS,
                    null
                )
                ?: return emptyList()

        return try {

            val array =
                JSONArray(raw)

            val result =
                mutableListOf<ChatConversation>()

            for (i in 0 until array.length()) {

                val conversationJson =
                    array.getJSONObject(i)

                val messagesJson =
                    conversationJson.optJSONArray(
                        "messages"
                    )

                val messages =
                    mutableListOf<ChatMessage>()

                if (messagesJson != null) {

                    for (
                        j in 0 until messagesJson.length()
                    ) {

                        val messageJson =
                            messagesJson.getJSONObject(j)

                        messages.add(
                            ChatMessage(
                                id =
                                    messageJson.optString(
                                        "id"
                                    ),

                                text =
                                    messageJson.optString(
                                        "text"
                                    ),

                                isUser =
                                    messageJson.optBoolean(
                                        "isUser"
                                    ),

                                timestamp =
                                    messageJson.optLong(
                                        "timestamp"
                                    )
                            )
                        )
                    }
                }

                result.add(
                    ChatConversation(
                        id =
                            conversationJson.optString(
                                "id"
                            ),

                        title =
                            conversationJson.optString(
                                "title",
                                "新对话"
                            ),

                        messages =
                            messages,

                        createdAt =
                            conversationJson.optLong(
                                "createdAt"
                            ),

                        updatedAt =
                            conversationJson.optLong(
                                "updatedAt"
                            )
                    )
                )
            }

            result

        } catch (_: Exception) {

            emptyList()
        }
    }

    fun deleteConversation(
        context: Context,
        conversationId: String
    ) {

        val conversations =
            loadConversations(context)

        val remaining =
            conversations.filter {
                it.id != conversationId
            }

        saveConversations(
            context,
            remaining
        )
    }

    fun clearAll(
        context: Context
    ) {

        prefs(context)
            .edit()
            .remove(KEY_CONVERSATIONS)
            .apply()
    }
}