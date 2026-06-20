package com.nature.browser.devtools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun JsConsole(logs: List<String>, onExecute: (String) -> Unit) {
    var command by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(Color.Black.copy(alpha = 0.8f))
            .padding(8.dp)
    ) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(logs) { log ->
                Text(log, color = Color.Green, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            TextField(
                value = command,
                onValueChange = { command = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("js >", color = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
            Button(onClick = {
                onExecute(command)
                command = ""
            }) {
                Text("Run")
            }
        }
    }
}
