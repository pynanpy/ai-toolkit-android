package com.pynanpy.aitoolkit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    conversation: ChatConversation,
    settings: AppSettings,
    onConversationChanged: (ChatConversation) -> Unit,
    onOpenSettings: () -> Unit,
    onNewConversation: () -> Unit
) {

    var input by remember {
        mutableStateOf("")
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    val listState =
        rememberLazyListState()

    val scope =
        rememberCoroutineScope()

    LaunchedEffect(
        conversation.messages.size
    ) {

        if (
            conversation.messages.isNotEmpty()
        ) {

            listState.animateScrollToItem(
                conversation.messages.lastIndex
            )
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .imePadding()
    ) {

        ChatTopBar(
            title =
                conversation.title,

            model =
                settings.model,

            onNewConversation =
                onNewConversation,

            onOpenSettings =
                onOpenSettings
        )

        LazyColumn(

            state =
                listState,

            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(
                        horizontal = 14.dp
                    ),

            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            items(
                items =
                    conversation.messages,

                key = {
                    it.id
                }
            ) { message ->

                MessageBubble(
                    message = message
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

                    value =
                        input,

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

                        if (
                            userText.isEmpty()
                        ) {
                            return@Button
                        }

                        if (
                            settings.apiKey.isBlank()
                        ) {

                            val errorMessage =
                                ChatMessage(
                                    text =
                                        "请先打开「设置」，填写 API Key。",
                                    isUser = false
                                )

                            onConversationChanged(
                                conversation.copy(
                                    messages =
                                        conversation.messages +
                                            errorMessage,

                                    updatedAt =
                                        System.currentTimeMillis()
                                )
                            )

                            return@Button
                        }

                        input = ""

                        val userMessage =
                            ChatMessage(
                                text =
                                    userText,

                                isUser =
                                    true
                            )

                        val conversationWithUser =
                            conversation.copy(
                                messages =
                                    conversation.messages +
                                        userMessage,

                                title =
                                    if (
                                        conversation.messages.isEmpty()
                                    ) {
                                        createTitle(
                                            userText
                                        )
                                    } else {
                                        conversation.title
                                    },

                                updatedAt =
                                    System.currentTimeMillis()
                            )

                        val aiMessage =
                            ChatMessage(
                                text = "",
                                isUser = false
                            )

                        val conversationWithPlaceholder =
                            conversationWithUser.copy(
                                messages =
                                    conversationWithUser.messages +
                                        aiMessage,

                                updatedAt =
                                    System.currentTimeMillis()
                            )

                        onConversationChanged(
                            conversationWithPlaceholder
                        )

                        isLoading = true

                        scope.launch {

                            val result =
                                ApiClient.sendMessageStream(

                                    baseUrl =
                                        settings.baseUrl,

                                    apiKey =
                                        settings.apiKey,

                                    model =
                                        settings.model,

                                    messages =
                                        conversationWithUser.messages,

                                    onChunk = { chunk ->

val updated =
                                            appendToMessage(
                                                conversationWithPlaceholder,
                                                aiMessage.id,
                                                chunk
                                            )

                                        onConversationChanged(
                                            updated
                                        )
                                    }
                                )

                            result.onFailure { error ->

                                val errorText =
                                    "请求失败：\n" +
                                        (
                                            error.message
                                                ?: "未知错误"
                                        )

                                val failed =
                                    replaceMessage(
                                        conversationWithPlaceholder,
                                        aiMessage.id,
                                        errorText
                                    )

                                onConversationChanged(
                                    failed
                                )
                            }

                            isLoading = false
                        }
                    }
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

private fun createTitle(
    text: String
): String {

    val clean =
        text
            .replace("\n", " ")
            .trim()

    return if (
        clean.length <= 18
    ) {
        clean
    } else {
        clean.take(18) + "…"
    }
}

private fun appendToMessage(
    conversation: ChatConversation,
    messageId: String,
    chunk: String
): ChatConversation {

    return conversation.copy(

        messages =
            conversation.messages.map {

                if (
                    it.id == messageId
                ) {

                    it.copy(
                        text =
                            it.text + chunk
                    )

                } else {
                    it
                }
            },

        updatedAt =
            System.currentTimeMillis()
    )
}

private fun replaceMessage(
    conversation: ChatConversation,
    messageId: String,
    text: String
): ChatConversation {

    return conversation.copy(

        messages =
            conversation.messages.map {

                if (
                    it.id == messageId
                ) {
                    it.copy(
                        text = text
                    )
                } else {
                    it
                }
            },

        updatedAt =
            System.currentTimeMillis()
    )
}

@Composable
private fun ChatTopBar(
    title: String,
    model: String,
    onNewConversation: () -> Unit,
    onOpenSettings: () -> Unit
) {

    Surface(
        tonalElevation = 2.dp
    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 14.dp,
                        vertical = 10.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text =
                        if (
                            title.isBlank()
                        ) {
                            "新对话"
                        } else {
                            title
                        },

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,

                    fontWeight =
                        FontWeight.SemiBold
                )

                Text(
                    text =
                        model,

                    style =
                        MaterialTheme
                            .typography
                            .labelSmall
                )
            }

            TextButton(
                onClick =
                    onNewConversation
            ) {
                Text("新对话")
            }

            TextButton(
                onClick =
                    onOpenSettings
            ) {
                Text("设置")
            }
        }
    }
}

@Composable
private fun ThinkingIndicator() {

    val transition =
        rememberInfiniteTransition(
            label = "thinking"
        )

    val alpha by
        transition.animateFloat(
            initialValue =
                0.35f,

            targetValue =
                1f,

            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(700),

                    repeatMode =
                        RepeatMode.Reverse
                ),

            label =
                "thinkingAlpha"
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

            strokeWidth =
                2.dp
        )

        Spacer(
            modifier =
                Modifier.width(8.dp)
        )

        Text(
            text =
                "AI 正在思考...",

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
private fun MessageBubble(
    message: ChatMessage
) {

    AnimatedVisibility(

        visible = true,

        enter =
            fadeIn(
                animationSpec =
                    tween(180)
            ) +
                slideInVertically(
                    animationSpec =
                        tween(240),

                    initialOffsetY = {
                        it / 5
                    }
                )
    ) {

        Row(

            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                if (message.isUser) {
                    Arrangement.End
                } else {
                    Arrangement.Start
                }
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
                    Modifier.fillMaxWidth(
                        0.90f
                    ),

                shape =
                    RoundedCornerShape(18.dp),

                color =
                    bubbleColor
            ) {

                if (message.isUser) {

                    Text(
                        text =
                            message.text,

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

                } else {

                    MarkdownContent(
                        text =
                            message.text,

                        textColor =
                            textColor
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkdownContent(
    text: String,
    textColor: Color
) {

    val lines =
        text.split("\n")

    Column(

        modifier =
            Modifier.padding(
                horizontal = 16.dp,
                vertical = 12.dp
            ),

        verticalArrangement =
            Arrangement.spacedBy(6.dp)
    ) {

        var inCodeBlock =
            false

        var codeText =
            ""

        for (line in lines) {

            if (
                line.trim()
                    .startsWith("```")
            ) {

                if (!inCodeBlock) {

                    inCodeBlock =
                        true

                    codeText =
                        ""

                } else {

                    CodeBlock(
                        code =
                            codeText
                    )

                    inCodeBlock =
                        false
                }

                continue
            }

            if (inCodeBlock) {

                codeText +=
                    line + "\n"

                continue
            }

            when {

                line.startsWith("### ") -> {

                    Text(
                        text =
                            line.removePrefix(
                                "### "
                            ),

                        color =
                            textColor,

                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,

                        fontWeight =
                            FontWeight.Bold
                    )
                }

                line.startsWith("## ") -> {

                    Text(
                        text =
                            line.removePrefix(
                                "## "
                            ),

                        color =
                            textColor,

                        style =
                            MaterialTheme
                                .typography
                                .titleLarge,

                        fontWeight =
                            FontWeight.Bold
                    )
                }

                line.startsWith("# ") -> {

                    Text(
                        text =
                            line.removePrefix(
                                "# "
                            ),

                        color =
                            textColor,

                        style =
                            MaterialTheme
                                .typography
                                .headlineSmall,

                        fontWeight =
                            FontWeight.Bold
                    )
                }

                line.startsWith("- ") ||
                    line.startsWith("* ") -> {

                    Row {

                        Text(
                            text =
                                "•",

                            color =
                                textColor
                        )

                        Spacer(
                            modifier =
                                Modifier.width(8.dp)
                        )

                        MarkdownText(
                            text =
                                line.drop(2),

                            color =
                                textColor
                        )
                    }
                }

                line.isBlank() -> {

Spacer(
                        modifier =
                            Modifier.width(1.dp)
                    )
                }

                else -> {

                    MarkdownText(
                        text =
                            line,

                        color =
                            textColor
                    )
                }
            }
        }

        if (
            inCodeBlock &&
            codeText.isNotEmpty()
        ) {

            CodeBlock(
                code =
                    codeText
            )
        }
    }
}

@Composable
private fun MarkdownText(
    text: String,
    color: Color
) {

    val annotated =
        buildAnnotatedString {

            var position =
                0

            while (
                position < text.length
            ) {

                if (
                    position + 1 <
                        text.length &&
                    text[position] == '*' &&
                    text[position + 1] == '*'
                ) {

                    val end =
                        text.indexOf(
                            "**",
                            position + 2
                        )

                    if (end >= 0) {

                        val content =
                            text.substring(
                                position + 2,
                                end
                            )

                        pushStyle(
                            SpanStyle(
                                fontWeight =
                                    FontWeight.Bold
                            )
                        )

                        append(content)

                        pop()

                        position =
                            end + 2

                        continue
                    }
                }

                if (
                    text[position] == '`'
                ) {

                    val end =
                        text.indexOf(
                            "`",
                            position + 1
                        )

                    if (end >= 0) {

                        val content =
                            text.substring(
                                position + 1,
                                end
                            )

                        pushStyle(
                            SpanStyle(
                                fontFamily =
                                    FontFamily.Monospace,

                                fontSize =
                                    14.sp
                            )
                        )

                        append(content)

                        pop()

                        position =
                            end + 1

                        continue
                    }
                }

                append(
                    text[position]
                )

                position++
            }
        }

    Text(
        text =
            annotated,

        color =
            color,

        style =
            MaterialTheme
                .typography
                .bodyLarge
    )
}

@Composable
private fun CodeBlock(
    code: String
) {

    Surface(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(12.dp),

        tonalElevation =
            4.dp
    ) {

        Box(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme
                            .colorScheme
                            .surface
                    )
                    .padding(14.dp)
        ) {

            Text(
                text =
                    code.trimEnd(),

                fontFamily =
                    FontFamily.Monospace,

                fontSize =
                    13.sp,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface
            )
        }
    }
}