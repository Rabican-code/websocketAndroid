package com.example.piesockettest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Input field for WebSocket URL
        TextField(
            value = apiTest,
            onValueChange = { apiTest = it },
            label = { Text("API Test (WebSocket URL)") }
        )
        Button(onClick = {
            connectToApiTest(apiTest)
            response = "Connecting to $apiTest"
        }) {
            Text("Connect to API Test")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input field for message
        TextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Message") }
        )
        Button(onClick = {
            if (isConnected()) {
                sendMessage(message)
                response = "Message sent"
            } else {
                response = "Not connected"
            }
        }) {
            Text("Send Message")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display status message
        Text(text = response)


        // Display received message
        Text(text = " ${receivedMessage()}", modifier = Modifier.padding(top = 16.dp))

        // Display sent messages
        Text(text = "", modifier = Modifier.padding(top = 16.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            sentMessages().forEach { msg ->
                Text(text = msg,modifier = Modifier.padding(top = 16.dp))
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
