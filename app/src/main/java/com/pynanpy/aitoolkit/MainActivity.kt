package com.pynanpy.aitoolkit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
                ChatScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen() {

    var input by remember {
        mutableStateOf("")
    }

    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMessage(
                    text = "你好，我是 AI Toolkit。现在可以开始聊天了。",
                    isUser = false
                )
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("AI Toolkit")
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
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(messages) { message ->

                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Text(
                            text = if (message.isUser) "你" else "AI",
                            style = MaterialTheme.typography.labelMedium
                        )

                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.Center
            ) {

                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text("输入消息...")
                    },
                    maxLines = 4
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Button(
                    onClick = {

                        if (input.isNotBlank()) {

                            val userMessage = input.trim()

                            messages = messages + ChatMessage(
                                text = userMessage,
                                isUser = true
                            )

                            messages = messages + ChatMessage(
                                text = "这是测试回复。下一阶段我们会接入真正的 AI API。",
                                isUser = false
                            )

                            input = ""
                        }
                    }
                ) {
                    Text("发送")
                }
            }
        }
    }
}