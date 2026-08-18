package com.pynanpy.aitoolkit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContent {
            AIToolkitTheme {
                AIToolkitApp()
            }
        }
    }
}

@Composable
private fun AIToolkitApp() {

    val context =
        androidx.compose.ui.platform.LocalContext.current

    var conversations by remember {
        mutableStateOf(
            emptyList<ChatConversation>()
        )
    }

    var currentConversationId by remember {
        mutableStateOf<String?>(null)
    }

    var settings by remember {
        mutableStateOf(
            AppSettings()
        )
    }

    var isLoaded by remember {
        mutableStateOf(false)
    }

    var showSettings by remember {
        mutableStateOf(false)
    }

    var saveSettingsRequest by remember {
        mutableStateOf<AppSettings?>(null)
    }

    /*
     * 读取本地设置和聊天记录
     */
    LaunchedEffect(Unit) {

        settings =
            SettingsRepository.load(
                context
            )

        val saved =
            ChatRepository.loadConversations(
                context
            )

        if (saved.isEmpty()) {

            val conversation =
                ChatConversation()

            conversations =
                listOf(conversation)

            currentConversationId =
                conversation.id

            ChatRepository.saveConversations(
                context,
                conversations
            )

        } else {

            conversations =
                saved

            currentConversationId =
                saved.maxByOrNull {
                    it.updatedAt
                }?.id
        }

        isLoaded = true
    }

    /*
     * 保存设置
     */
    LaunchedEffect(saveSettingsRequest) {

        val newSettings =
            saveSettingsRequest
                ?: return@LaunchedEffect

        SettingsRepository.save(
            context,
            newSettings
        )

        saveSettingsRequest = null
    }

    if (!isLoaded) {

        Box(
            modifier =
                Modifier.fillMaxSize(),

            contentAlignment =
                Alignment.Center
        ) {

            CircularProgressIndicator()
        }

        return
    }

    /*
     * 设置页面
     */
    if (showSettings) {

        SettingsScreen(

            initialBaseUrl =
                settings.baseUrl,

            initialApiKey =
                settings.apiKey,

            initialModel =
                settings.model,

            onSave = { baseUrl, apiKey, model ->

                val newSettings =
                    AppSettings(
                        baseUrl =
                            baseUrl,

                        apiKey =
                            apiKey,

                        model =
                            model
                    )

                settings =
                    newSettings

                saveSettingsRequest =
                    newSettings

                showSettings = false
            },

            onBack = {
                showSettings = false
            }
        )

        return
    }

    /*
     * 获取当前对话
     */
    val currentConversation =
        conversations.firstOrNull {
            it.id ==
                currentConversationId
        }

    if (currentConversation == null) {

        val newConversation =
            ChatConversation()

        conversations =
            conversations + newConversation

        currentConversationId =
            newConversation.id

        ChatRepository.saveConversations(
            context,
            conversations
        )

        return
    }

    /*
     * 聊天页面
     */
    ChatScreen(

        conversation =
            currentConversation,

        settings =
            settings,

        onConversationChanged = { updated ->

            conversations =
                conversations.map {

                    if (
                        it.id ==
                            updated.id
                    ) {
                        updated
                    } else {
                        it
                    }
                }

            ChatRepository.saveConversations(
                context,
                conversations
            )
        },

        onOpenSettings = {
            showSettings = true
        },

        onNewConversation = {

            val newConversation =
                ChatConversation()

            conversations =
                conversations +
                    newConversation

            currentConversationId =
                newConversation.id

            ChatRepository.saveConversations(
                context,
                conversations
            )
        }
    )
}