package com.example.piesockettest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class MainActivity : ComponentActivity() {
    private var webSocket: WebSocket? = null
    private var isConnected by mutableStateOf(false)
    private var receivedMessage by mutableStateOf("")
    private var sentMessages = mutableStateListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp(
                connectToApiTest = { url -> connectToWebSocket(url) },
                sendMessage = { message -> sendMessage(message) },
                isConnected = { isConnected },
                receivedMessage = { receivedMessage },
                sentMessages = { sentMessages }
            )
        }
    }

    private fun connectToWebSocket(url: String) {
        if (webSocket != null) {
            webSocket?.close(1000, "Reconnecting to new WebSocket")
        }

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        val listener = EchoWebSocketListener(
            onOpen = { onConnectionOpened() },
            onMessageReceived = { message -> onMessageReceived(message) }
        )
        webSocket = client.newWebSocket(request, listener)
        client.dispatcher.executorService.shutdown()
    }

    private fun onConnectionOpened() {
        isConnected = true
    }

    private fun sendMessage(message: String) {
        if (isConnected) {
            webSocket?.send(message)
            sentMessages.add("Sent: $message")
        }
    }

    private fun onMessageReceived(message: String) {
        receivedMessage = message
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket?.close(1000, "Activity destroyed")
    }

    private class EchoWebSocketListener(
        private val onOpen: () -> Unit,
        private val onMessageReceived: (String) -> Unit
    ) : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
            onOpen()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            onMessageReceived(text)
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            onMessageReceived(bytes.utf8())
        }
    }
}

@Composable
fun MyApp(
    connectToApiTest: (String) -> Unit,
    sendMessage: (String) -> Unit,
    isConnected: () -> Boolean,
    receivedMessage: () -> String,
    sentMessages: () -> List<String>
) {
    var message by remember { mutableStateOf("") }
    var apiTest by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "WebSocket Tester",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 16.dp),
            fontSize = 24.sp
        )

        // Connection Status
        if (isConnected()) {
            Text(
                text = "Connected",
                color = Color.Green,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        } else {
            Text(
                text = "Not Connected",
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Input field for WebSocket URL
        TextField(
            value = apiTest,
            onValueChange = { apiTest = it },
            label = { Text("API Test (WebSocket URL)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
        Button(
            onClick = {
                connectToApiTest(apiTest)
                response = "Connecting to $apiTest"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Connect to API Test")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Input field for message and send button in row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Message") },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )
            Button(
                onClick = {
                    if (isConnected()) {
                        sendMessage(message)
                        response = "Message sent"
                    } else {
                        response = "Not connected"
                    }
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Send")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display status message
        Text(text = response, color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp))

        Divider(modifier = Modifier.fillMaxWidth(), color = Color.LightGray)

        // Display sent messages
        if (sentMessages().isNotEmpty()) {
            Text(text = "Sent Messages:", fontSize = 20.sp, modifier = Modifier.padding(vertical = 8.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                sentMessages().forEach { msg ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),

                    ) {
                        Text(
                            text = msg,
                            modifier = Modifier.padding(8.dp),
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Display received message
        if (receivedMessage().isNotEmpty()) {
            Text(text = "Received Message:", fontSize = 20.sp, modifier = Modifier.padding(vertical = 8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = receivedMessage(),
                    modifier = Modifier.padding(8.dp),
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MyApp(
        connectToApiTest = {},
        sendMessage = {},
        isConnected = { false },
        receivedMessage = { "" },
        sentMessages = { emptyList() }
    )
}
