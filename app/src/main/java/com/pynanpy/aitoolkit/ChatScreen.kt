package com.pynanpy.aitoolkit

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

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
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import kotlinx.coroutines.Job
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

    var currentJob by remember {
        mutableStateOf<Job?>(null)
    }

    val listState =
        rememberLazyListState()

    val scope =
        rememberCoroutineScope()

    val context =
        LocalContext.current


    /*
     * 自动滚动到底部
     */
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

            onNewConversation = {

                if (!isLoading) {
                    onNewConversation()
                }
            },

            onOpenSettings = {

                if (!isLoading) {
                    onOpenSettings()
                }
            }
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

                    message =
                        message,

                    onCopy = {

                        copyToClipboard(
                            context,
                            message.text
                        )
                    },

                    onRegenerate = {

                        if (
                            !isLoading &&
                            !message.isUser
                        ) {

                            regenerateMessage(

                                conversation =
                                    conversation,

                                message =
                                    message,

                                settings =
                                    settings,

                                onConversationChanged =
                                    onConversationChanged,

                                setLoading = {
                                    isLoading = it
                                },

                                setJob = {
                                    currentJob = it
                                },

                                scope =
                                    scope
                            )
                        }
                    }
                )
            }


            if (isLoading) {

                item {

                    ThinkingIndicator()
                }
            }
        }


        InputBar(

            input =
                input,

            isLoading =
                isLoading,

            onInputChanged = {
                input = it
            },

            onSend = {

                val userText =
                    input.trim()

                if (
                    userText.isEmpty()
                ) {
                    return@InputBar
                }

                if (
                    settings.apiKey.isBlank()
                ) {

                    val errorMessage =
                        ChatMessage(
                            text =
                                "请先打开「设置」，填写 API Key。",
                            isUser =
                                false
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

                    return@InputBar
                }


                input = ""


                val userMessage =
                    ChatMessage(
                        text =
                            userText,

                        isUser =
                            true
                    )


                val assistantMessage =
                    ChatMessage(
                        text =
                            "",

                        isUser =
                            false
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


                val conversationWithAssistant =

                    conversationWithUser.copy(

                        messages =
                            conversationWithUser.messages +
                                assistantMessage,

                        updatedAt =
                            System.currentTimeMillis()
                    )


                onConversationChanged(
                    conversationWithAssistant
                )


                isLoading = true


                currentJob =
                    scope.launch {

                        var generatedText =
                            ""


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

                                    generatedText +=
                                        chunk


                                    val updated =
                                        replaceMessage(

                                            conversationWithAssistant,

                                            assistantMessage.id,

                                            generatedText
                                        )


                                    onConversationChanged(
                                        updated
                                    )
                                }
                            )


                        result.onFailure { error ->

                            val errorText =

                                if (
                                    error.message
                                        .isNullOrBlank()
                                ) {

                                    "请求失败：未知错误"

                                } else {

                                    "请求失败：${error.message}"
                                }


                            val failed =
                                replaceMessage(

                                    conversationWithAssistant,

                                    assistantMessage.id,

                                    errorText
                                )


                            onConversationChanged(
                                failed
                            )
                        }


                        isLoading = false

                        currentJob = null
                    }
            },

            onStop = {

                currentJob?.cancel()

                currentJob = null

                isLoading = false
            }
        )
    }
}


/*
 * 输入区域
 */
@Composable
private fun InputBar(
    input: String,
    isLoading: Boolean,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit
) {

    Surface(

        modifier =
            Modifier.fillMaxWidth(),

        tonalElevation =
            3.dp
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

                onValueChange =
                    onInputChanged,

                modifier =
                    Modifier.weight(1f),

                placeholder = {

                    Text(
                        "输入消息..."
                    )
                },

                shape =
                    RoundedCornerShape(24.dp),

                maxLines =
                    5,

                enabled =
                    !isLoading
            )


            Spacer(
                modifier =
                    Modifier.width(8.dp)
            )


            Button(

                enabled =
                    if (isLoading) {
                        true
                    } else {
                        input.isNotBlank()
                    },

                onClick = {

                    if (isLoading) {

                        onStop()

                    } else {

                        onSend()
                    }
                }

            ) {

                Text(

                    if (isLoading) {
                        "停止"
                    } else {
                        "发送"
                    }
                )
            }
        }
    }
}


/*
 * 重新生成 AI 回复
 */
private fun regenerateMessage(

    conversation: ChatConversation,

    message: ChatMessage,

    settings: AppSettings,

    onConversationChanged:
        (ChatConversation) -> Unit,

    setLoading:
        (Boolean) -> Unit,

    setJob:
        (Job?) -> Unit,

    scope:
        kotlinx.coroutines.CoroutineScope
) {

    val assistantIndex =
        conversation.messages
            .indexOfFirst {
                it.id == message.id
            }

 if (
        assistantIndex <= 0
    ) {
        return
    }


    val userMessage =
        conversation.messages
            .getOrNull(
                assistantIndex - 1
            )
            ?: return


    if (
        !userMessage.isUser
    ) {
        return
    }


    /*
     * 保留到这个用户问题之前的全部历史。
     */
    val previousMessages =
        conversation.messages
            .take(
                assistantIndex - 1
            )


    val newAssistantMessage =
        ChatMessage(

            text =
                "",

            isUser =
                false
        )


    val newConversation =

        conversation.copy(

            messages =
                previousMessages +
                    userMessage +
                    newAssistantMessage,

            updatedAt =
                System.currentTimeMillis()
        )


    onConversationChanged(
        newConversation
    )


    setLoading(true)


    val job =
        scope.launch {

            var generatedText =
                ""


            val result =
                ApiClient.sendMessageStream(

                    baseUrl =
                        settings.baseUrl,

                    apiKey =
                        settings.apiKey,

                    model =
                        settings.model,

                    messages =
                        previousMessages +
                            userMessage,

                    onChunk = { chunk ->

                        generatedText +=
                            chunk


                        val updated =
                            replaceMessage(

                                newConversation,

                                newAssistantMessage.id,

                                generatedText
                            )


                        onConversationChanged(
                            updated
                        )
                    }
                )


            result.onFailure { error ->

                val errorText =

                    if (
                        error.message
                            .isNullOrBlank()
                    ) {

                        "请求失败：未知错误"

                    } else {

                        "请求失败：${error.message}"
                    }


                val failed =
                    replaceMessage(

                        newConversation,

                        newAssistantMessage.id,

                        errorText
                    )


                onConversationChanged(
                    failed
                )
            }


            setLoading(false)

            setJob(null)
        }


    setJob(job)
}


/*
 * 自动生成对话标题
 */
private fun createTitle(
    text: String
): String {

    val clean =
        text
            .replace(
                "\n",
                " "
            )
            .trim()


    return if (
        clean.length <= 18
    ) {

        clean

    } else {

        clean.take(18) + "…"
    }
}


/*
 * 替换指定消息
 */
private fun replaceMessage(

    conversation:
        ChatConversation,

    messageId:
        String,

    text:
        String

): ChatConversation {

    return conversation.copy(

        messages =
            conversation.messages.map {

                if (
                    it.id == messageId
                ) {

                    it.copy(
                        text =
                            text
                    )

                } else {

                    it
                }
            },

        updatedAt =
            System.currentTimeMillis()
    )
}


/*
 * 复制文本
 */
private fun copyToClipboard(

    context:
        Context,

    text:
        String
) {

    if (
        text.isBlank()
    ) {
        return
    }


    val clipboard =
        context.getSystemService(
            Context.CLIPBOARD_SERVICE
        ) as ClipboardManager


    clipboard.setPrimaryClip(

        ClipData.newPlainText(

            "AI 回复",

            text
        )
    )
}


/*
 * 顶部栏
 */
@Composable
private fun ChatTopBar(

    title:
        String,

    model:
        String,

    onNewConversation:
        () -> Unit,

    onOpenSettings:
        () -> Unit
) {

    Surface(

        tonalElevation =
            2.dp
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

                Text(
                    "新对话"
                )
            }


            TextButton(

                onClick =
                    onOpenSettings

            ) {

                Text(
                    "设置"
                )
            }
        }
    }
}


/*
 * 思考动画
 */
@Composable
private fun ThinkingIndicator() {

    val transition =
        rememberInfiniteTransition(
            label =
                "thinking"
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
                        tween(
                            700
                        ),

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
                Modifier.width(
                    20.dp
                ),

            strokeWidth =
                2.dp
        )


        Spacer(

            modifier =
                Modifier.width(
                    8.dp
                )
        )


        Text(

            text =
                "AI 正在思考...",

            modifier =
                Modifier.graphicsLayer {

                    this.alpha =
                        alpha
                },

            style =
                MaterialTheme
                    .typography
                    .bodyMedium
        )
    }
}


/*
 * 消息气泡
 */
@Composable
private fun MessageBubble(

    message:
        ChatMessage,

    onCopy:
        () -> Unit,

    onRegenerate:
        () -> Unit
) {

    AnimatedVisibility(

        visible =
            true,

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

        Column(

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =

                    if (
                        message.isUser
                    ) {

                        Arrangement.End

                    } else {

                        Arrangement.Start
                    }
            ) {

                val bubbleColor =

                    if (
                        message.isUser
                    ) {

                        MaterialTheme
                            .colorScheme
                            .primary

                    } else {

                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                    }


                val textColor =

                    if (
                        message.isUser
                    ) {

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
                        RoundedCornerShape(
                            18.dp
                        ),

                    color =
                        bubbleColor
                ) {

                    if (
                        message.isUser
                    ) {

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


            /*
             * AI 回复操作按钮
             */
            if (
                !message.isUser &&
                message.text.isNotBlank()
            ) {

                Row(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 8.dp
                            )
                ) {

                    TextButton(
                        onClick =
                            onCopy
                    ) {

                        Text(
                            "复制"
                        )
                    }


                    TextButton(
                        onClick =
                            onRegenerate
                    ) {

                        Text(
                            "重新生成"
                        )
                    }
                }
            }
        }
    }
}


/*
 * Markdown 内容
 */
@Composable
private fun MarkdownContent(

    text:
        String,

    textColor:
        Color
) {

    val lines =
        text.split(
            "\n"
        )


    Column(

        modifier =
            Modifier.padding(
                horizontal = 16.dp,
                vertical = 12.dp
            ),

        verticalArrangement =
            Arrangement.spacedBy(
                6.dp
            )
    ) {

        var inCodeBlock =
            false

        var codeText =
            ""


        for (
            line in lines
        ) {

            /*
             * 代码块
             */
            if (
                line.trim()
                    .startsWith(
                        "```"
                    )
            ) {

                if (
                    !inCodeBlock
                ) {

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


            if (
                inCodeBlock
            ) {

                codeText +=
                    line + "\n"

                continue
            }


            when {

                /*
                 * ### 标题
                 */
                line.startsWith(
                    "### "
                ) -> {

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


                /*
                 * ## 标题
                 */

line.startsWith(
                    "## "
                ) -> {

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


                /*
                 * # 标题
                 */
                line.startsWith(
                    "# "
                ) -> {

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


                /*
                 * 无序列表
                 */
                line.startsWith(
                    "- "
                ) ||
                    line.startsWith(
                        "* "
                    ) -> {

                    Row {

                        Text(

                            text =
                                "•",

                            color =
                                textColor
                        )


                        Spacer(

                            modifier =
                                Modifier.width(
                                    8.dp
                                )
                        )


                        MarkdownText(

                            text =
                                line.drop(
                                    2
                                ),

                            color =
                                textColor
                        )
                    }
                }


                /*
                 * 空行
                 */
                line.isBlank() -> {

                    Spacer(

                        modifier =
                            Modifier.height(
                                1.dp
                            )
                    )
                }


                /*
                 * 普通文本
                 */
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


        /*
         * 未闭合代码块
         */
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


/*
 * Markdown 行内格式
 */
@Composable
private fun MarkdownText(

    text:
        String,

    color:
        Color
) {

    val annotated =
        buildAnnotatedString {

            var position =
                0


            while (
                position < text.length
            ) {

                /*
                 * **粗体**
                 */
                if (

                    position + 1 <
                        text.length &&

                    text[position] ==
                        '*' &&

                    text[position + 1] ==
                        '*'
                ) {

                    val end =
                        text.indexOf(

                            "**",

                            position + 2
                        )


                    if (
                        end >= 0
                    ) {

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


                        append(
                            content
                        )


                        pop()


                        position =
                            end + 2


                        continue
                    }
                }


                /*
                 * `行内代码`
                 */
                if (
                    text[position] ==
                        '`'
                ) {

                    val end =
                        text.indexOf(

                            "`",

                            position + 1
                        )


                    if (
                        end >= 0
                    ) {

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


                        append(
                            content
                        )


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


/*
 * 代码块
 */
@Composable
private fun CodeBlock(

    code:
        String
) {

    Surface(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                12.dp
            ),

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
                    .padding(
                        14.dp
                    )
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