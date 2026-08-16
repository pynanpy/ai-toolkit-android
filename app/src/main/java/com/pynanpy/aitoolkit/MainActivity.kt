package com.pynanpy.aitoolkit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

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

if (showSettings) {

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
            settings = settings,
            onOpenSettings = {
                showSettings = true
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    settings: AppSettings,
    onOpenSettings: () -> Unit
) {

    var input by remember {
        mutableStateOf("")
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    var messages by remember {

        mutableStateOf(

            listOf(

                ChatMessage(
                    text =
                        "你好，我是 AI Toolkit。\n\n" +
                        "你可以在右上角设置 API，然后开始聊天。",
                    isUser = false
                )
            )
        )
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

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
                            style =
                                MaterialTheme
                                    .typography
                                    .titleLarge
                        )

                        Text(
                            text = settings.model,
                            style =
                                MaterialTheme
                                    .typography
                                    .labelSmall
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

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .imePadding()
        ) {

            LazyColumn(

                state = listState,

                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),

                verticalArrangement =
                    Arrangement.spacedBy(10.dp)

            ) {

                itemsIndexed(messages) { index, message ->

                    MessageBubble(
                        message = message,
                        animate = index == messages.lastIndex
                    )
                }

                if (isLoading) {

                    item {

                        ThinkingIndicator()
                    }
                }
            }

Surface(

                modifier =
                    Modifier.fillMaxWidth(),

                tonalElevation = 3.dp
            ) {

                Row(

                    modifier =
                        Modifier
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

                        modifier =
                            Modifier.weight(1f),

                        placeholder = {
                            Text("输入消息...")
                        },

                        shape =
                            RoundedCornerShape(24.dp),

                        maxLines = 5,

                        enabled =
                            !isLoading
                    )

                    Spacer(
                        modifier =
                            Modifier.width(8.dp)
                    )

                    Button(

                        enabled =
                            input.isNotBlank() &&
                            !isLoading,

                        onClick = {

                            val userText =
                                input.trim()

                            if (userText.isEmpty()) {
                                return@Button
                            }

                            if (settings.apiKey.isBlank()) {

                                messages =
                                    messages +
                                        ChatMessage(
                                            text =
                                                "请先打开「设置」，填写 API Key。",
                                            isUser = false
                                        )

                                return@Button
                            }

                            input = ""

                            messages =
                                messages +
                                    ChatMessage(
                                        text = userText,
                                        isUser = true
                                    )

                            messages =
                                messages +
                                    ChatMessage(
                                        text = "",
                                        isUser = false
                                    )

                            isLoading = true

                            scope.launch {

                                val aiIndex =
                                    messages.lastIndex

                                val result =
                                    ApiClient.sendMessageStream(

                                        baseUrl =
                                            settings.baseUrl,

                                        apiKey =
                                            settings.apiKey,

                                        model =
                                            settings.model,

                                        message =
                                            userText

                                    ) { chunk ->

                                        if (
                                            aiIndex <
                                            messages.size
                                        ) {

                                            val current =
                                                messages[aiIndex]

                                            messages =
                                                messages
                                                    .toMutableList()
                                                    .also {

                                                        it[aiIndex] =
                                                            current.copy(
                                                                text =
                                                                    current.text +
                                                                    chunk
                                                            )
                                                    }

                                            listState.animateScrollToItem(
                                                messages.lastIndex
                                            )
                                        }
                                    }

                                result.onFailure { error ->

                                    val current =
                                        messages[aiIndex]

                                    messages =
                                        messages
                                            .toMutableList()
                                            .also {

                                                it[aiIndex] =
                                                    current.copy(
                                                        text =
                                                            "请求失败：\n" +
                                                            (
                                                                error.message
                                                                    ?: "未知错误"
                                                            )
                                                    )
                                            }
                                }

                                isLoading = false
                            }
                        },

                        modifier =
                            Modifier.padding(top = 6.dp)

                    ) {

                        if (isLoading) {
                            Text("生成中")
                        } else {
                            Text("发送")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThinkingIndicator() {

    val transition =
        rememberInfiniteTransition(
            label = "thinking"
        )

    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,

        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(700),
                repeatMode =
                    RepeatMode.Reverse
            ),

        label = "thinkingAlpha"
    )

    Row(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 6.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        CircularProgressIndicator(

            modifier =
                Modifier.width(20.dp),

            strokeWidth = 2.dp
        )

        Spacer(
            modifier =
                Modifier.width(8.dp)
        )

        Text(
            text = "AI 正在思考...",
            modifier =
                Modifier.graphicsLayer {
                    this.alpha = alpha
                },
            style =
                MaterialTheme
                    .typography
                    .bodyMedium
        )
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    animate: Boolean
) {

    val horizontalArrangement =
        if (message.isUser) {
            Arrangement.End
        } else {
            Arrangement.Start
        }

    AnimatedVisibility(

        visible = true,

        enter =
            if (animate) {

                fadeIn(
                    animationSpec =
                        tween(220)
                ) +
                    slideInVertically(
                        animationSpec =
                            tween(280),
                        initialOffsetY = {
                            it / 5
                        }
                    )

            } else {

                fadeIn(
                    animationSpec =
                        tween(120)
                )
            }
    ) {

        Row(

            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                horizontalArrangement
        ) {

            val bubbleColor =
                if (message.isUser) {

                    MaterialTheme
                        .colorScheme
                        .primary

                } else {

                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
                }

            val textColor =
                if (message.isUser) {

                    MaterialTheme
                        .colorScheme
                        .onPrimary

                } else {

                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
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

                    text =
                        if (
                            message.text.isEmpty() &&
                            !message.isUser
                        ) {
                            "▌"
                        } else {
                            message.text
                        },

                    modifier =
                        Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 12.dp
                        ),

                    color =
                        textColor,

                    style =
                        MaterialTheme
                            .typography
                            .bodyLarge
                )
            }
        }
    }
}