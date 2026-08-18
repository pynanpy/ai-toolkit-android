package com.pynanpy.aitoolkit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ConversationDrawer(
    conversations: List<ChatConversation>,
    currentConversationId: String?,
    onSelectConversation: (String) -> Unit,
    onNewConversation: () -> Unit,
    onDeleteConversation: (String) -> Unit,
    onClearAll: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    var showClearDialog by remember {
        mutableStateOf(false)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        // 半透明遮罩
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(),
            exit = fadeOut()
        ) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color.Black.copy(
                            alpha = 0.35f
                        )
                    )
                    .clickable {
                        onDismiss()
                    }
            )
        }

        // 侧栏
        AnimatedVisibility(
            visible = true,
            enter =
                slideInHorizontally(
                    initialOffsetX = {
                        -it
                    }
                ) + fadeIn(),
            exit =
                slideOutHorizontally(
                    targetOffsetX = {
                        -it
                    }
                ) + fadeOut()
        ) {

            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.82f),
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                shape = RoundedCornerShape(
                    topEnd = 20.dp,
                    bottomEnd = 20.dp
                )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {

                    // 顶部标题
Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                vertical = 4.dp
                            ),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Column(
                            modifier =
                                Modifier.weight(1f)
                        ) {

                            Text(
                                text = "AI Toolkit",
                                style =
                                    MaterialTheme
                                        .typography
                                        .headlineSmall
                            )

                            Text(
                                text = "历史对话",
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )
                        }

                        TextButton(
                            onClick = onDismiss
                        ) {
                            Text("关闭")
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.padding(
                                6.dp
                            )
                    )

                    // 新对话按钮
                    Button(
                        onClick =
                            onNewConversation,
                        modifier =
                            Modifier.fillMaxWidth(),
                        shape =
                            RoundedCornerShape(
                                14.dp
                            )
                    ) {
                        Text("＋  新对话")
                    }

                    Spacer(
                        modifier =
                            Modifier.padding(
                                6.dp
                            )
                    )

                    Divider()

                    Spacer(
                        modifier =
                            Modifier.padding(
                                4.dp
                            )
                    )

                    // 对话数量
                    Text(
                        text =
                            "${conversations.size} 个对话",
                        style =
                            MaterialTheme
                                .typography
                                .labelMedium,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )

                    Spacer(
                        modifier =
                            Modifier.padding(
                                4.dp
                            )
                    )

                    // 历史列表
                    LazyColumn(
                        modifier =
                            Modifier.weight(1f),
                        verticalArrangement =
                            Arrangement.spacedBy(
                                6.dp
                            )
                    ) {

                        items(
                            items = conversations
                                .sortedByDescending {
                                    it.updatedAt
                                },
                            key = {
                                it.id
                            }
                        ) { conversation ->

ConversationItem(
                                conversation =
                                    conversation,

                                selected =
                                    conversation.id ==
                                        currentConversationId,

                                onClick = {
                                    onSelectConversation(
                                        conversation.id
                                    )
                                },

                                onDelete = {
                                    onDeleteConversation(
                                        conversation.id
                                    )
                                }
                            )
                        }
                    }

                    Divider()

                    Spacer(
                        modifier =
                            Modifier.padding(
                                4.dp
                            )
                    )

                    // 设置
                    TextButton(
                        onClick =
                            onOpenSettings,
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text("⚙  设置")
                    }

                    // 清空历史
                    TextButton(
                        onClick = {
                            showClearDialog = true
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "清空全部历史",
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .error
                        )
                    }
                }
            }
        }
    }

    // 清空确认框
    if (showClearDialog) {

        AlertDialog(
            onDismissRequest = {
                showClearDialog = false
            },

            title = {
                Text("清空全部历史？")
            },

            text = {
                Text(
                    "所有已保存的对话都会被删除，此操作无法撤销。"
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        showClearDialog = false
                        onClearAll()
                    }
                ) {
                    Text(
                        "清空",
                        color =
                            MaterialTheme
                                .colorScheme
                                .error
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showClearDialog = false
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun ConversationItem(
    conversation: ChatConversation,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {

    val background =
        if (selected) {
            MaterialTheme
                .colorScheme
                .secondaryContainer
        } else {
            Color.Transparent
        }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = background,
                shape = RoundedCornerShape(
                    14.dp
                )
            )
            .clickable {
                onClick()
            }
            .padding(
                horizontal = 12.dp,
                vertical = 11.dp
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
                        conversation.title.isBlank()
                    ) {
                        "新对话"
                    } else {
                        conversation.title
                    },
                style =
                    MaterialTheme
                        .typography
                        .bodyLarge,
                maxLines = 1
            )

            Spacer(
                modifier =
                    Modifier.padding(
                        2.dp
                    )
            )

            val messageCount =
                conversation.messages.size

            Text(
                text =
                    if (messageCount == 0) {
                        "暂无消息"
                    } else {
                        "$messageCount 条消息"
                    },
                style =
                    MaterialTheme
                        .typography
                        .labelSmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }

        Spacer(
            modifier =
                Modifier.width(4.dp)
        )

        TextButton(
            onClick = onDelete
        ) {
            Text(
                "删除",
                color =
                    MaterialTheme
                        .colorScheme
                        .error
            )
        }
    }
}