package com.omniweb.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniweb.app.data.AppDatabase
import com.omniweb.app.data.Settings
import com.omniweb.app.data.Shortcut

@Composable
fun HomeView(onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val settingsState by database.settingsDao().getSettings().collectAsState(initial = Settings())
    val settings = settingsState ?: Settings()

    var query by remember { mutableStateOf("") }
    val shortcuts = remember {
        mutableStateListOf(
            Shortcut("Google", "https://www.google.com"),
            Shortcut("YouTube", "https://www.youtube.com"),
            Shortcut("GitHub", "https://www.github.com"),
            Shortcut("Reddit", "https://www.reddit.com"),
            Shortcut("Wikipedia", "https://www.wikipedia.org"),
            Shortcut("Amazon", "https://www.amazon.com"),
            Shortcut("X", "https://x.com"),
            Shortcut("Instagram", "https://www.instagram.com")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
            .padding(top = 96.dp, start = 32.dp, end = 32.dp, bottom = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(MaterialTheme.colorScheme.primary)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Public, contentDescription = null, tint = Color.White, modifier = Modifier.fillMaxSize())
        }

        Spacer(modifier = Modifier.height(40.dp))
        Text(text = "Omni Browser", fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
        Spacer(modifier = Modifier.height(40.dp))

        val focusManager = LocalFocusManager.current
        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search or type URL", fontSize = 18.sp) },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                if (query.isNotEmpty()) {
                    var target = query
                    if (!target.contains(".") || target.contains(" ")) {
                        target = "${settings.searchEngine}${android.net.Uri.encode(target)}"
                    } else if (!target.startsWith("http")) {
                        target = "https://$target"
                    }
                    onNavigate(target)
                }
                focusManager.clearFocus()
            }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color(0xFFE5E7EB),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            )
        )

        Spacer(modifier = Modifier.height(64.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.height(240.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(40.dp),
            userScrollEnabled = false
        ) {
            items(shortcuts) { shortcut ->
                ShortcutItem(shortcut, onClick = { onNavigate(shortcut.url) })
            }
            item { AddShortcutItem() }
        }
    }
}

@Composable
fun ShortcutItem(shortcut: Shortcut, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFFE5E7EB)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Language, contentDescription = null, tint = Color.Gray)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = shortcut.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.DarkGray)
    }
}

@Composable
fun AddShortcutItem() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Add", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}
