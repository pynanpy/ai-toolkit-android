package com.pynanpy.aitoolkit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                App()
            }
        }
    }
}

@Composable
fun App() {

    val context = LocalContext.current

    var showSettings by remember {
        mutableStateOf(false)
    }

    var settings by remember {
        mutableStateOf(AppSettings())
    }

    var settingsToSave by remember {
        mutableStateOf<AppSettings?>(null)
    }

    var loaded by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        settings = SettingsRepository.load(context)
        loaded = true
    }

    LaunchedEffect(settingsToSave) {
        settingsToSave?.let {
            SettingsRepository.save(context, it)
            settingsToSave = null
        }
    }

    if (!loaded) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AI Toolkit",
                style = MaterialTheme.typography.titleLarge
            )
        }

        return
    }

    AnimatedContent(
        targetState = showSettings,
        label = "screen"
    ) { isSettings ->

        if (isSettings) {

            SettingsScreen(
                initialBaseUrl = settings.baseUrl,
                initialApiKey = settings.apiKey,
                initialModel = settings.model,

                onSave = { baseUrl, apiKey, model ->

                    val newSettings = AppSettings(
                        baseUrl = baseUrl,
                        apiKey = apiKey,
                        model = model
                    )

                    settings = newSettings
                    settingsToSave = newSettings
                    showSettings = false
                },

                onBack = {
                    showSettings = false
                }
            )

        } else {

            ChatScreen(
                onOpenSettings = {
                    showSettings = true
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onOpenSettings: () -> Unit
) {

    var input by remember {
        mutableStateOf("")
    }

    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMessage(
                    text = "你好，我是 AI Toolkit。\n\n可以在右上角的设置中配置 AI API。",
                    isUser = false
                )
            )
        )
    }

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(
                messages.lastIndex
            )
        }
    }

    Scaffold(
        topBar = {

            TopAppBar(
                title = {

                    Column {

                        Text(
                            text = "AI Toolkit",
                            style = MaterialTheme.typography.titleLarge
                        )

                        Text(
                            text = "AI Assistant",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },

                actions = {

                    TextButton(
                        onClick = onOpenSettings
                    ) {
                        Text("设置")
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {

            LazyColumn(
                state = listState,

                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),

                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                items(messages) { message ->

                    MessageBubble(
                        message = message
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 3.dp
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),

                    verticalAlignment =
                        Alignment.Bottom
                ) {

                    OutlinedTextField(
                        value = input,

                        onValueChange = {
                            input = it
                        },

                        modifier = Modifier.weight(1f),

                        placeholder = {
                            Text("输入消息...")
                        },

                        shape = RoundedCornerShape(24.dp),

                        maxLines = 5
                    )

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Button(
                        onClick = {

                            if (input.isNotBlank()) {

                                val userText =
                                    input.trim()

                                messages =
                                    messages +
                                        ChatMessage(
                                            text = userText,
                                            isUser = true
                                        )

                                messages =
                                    messages +
                                        ChatMessage(
                                            text =
                                                "这是测试回复。\n\n下一步我们会接入真正的 AI API。",
                                            isUser = false
                                        )

                                input = ""
                            }
                        },

                        modifier =
                            Modifier.padding(top = 6.dp)
                    ) {

                        Text("发送")
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage
) {

    val horizontalArrangement: Arrangement.Horizontal

    if (message.isUser) {

        horizontalArrangement =
            Arrangement.End

    } else {

        horizontalArrangement =
            Arrangement.Start
    }

    Row(
        modifier = Modifier.fillMaxWidth(),

        horizontalArrangement =
            horizontalArrangement
    ) {

        val bubbleColor =
            if (message.isUser) {

                MaterialTheme.colorScheme.primary

            } else {

                MaterialTheme.colorScheme.surfaceVariant
            }

        val textColor =
            if (message.isUser) {

                MaterialTheme.colorScheme.onPrimary

            } else {

                MaterialTheme.colorScheme.onSurfaceVariant
            }

        Surface(
            modifier =
                Modifier.fillMaxWidth(0.86f),

            shape =
                RoundedCornerShape(18.dp),

            color =
                bubbleColor
        ) {

            Text(
                text = message.text,

                modifier =
                    Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    ),

                color = textColor,

                style =
                    MaterialTheme.typography.bodyLarge
            )
        }
    }
}