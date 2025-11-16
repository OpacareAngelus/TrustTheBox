package com.pTech.trustTheBox.ui.theme.screens.mediaScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pTech.trustTheBox.model.MItem
import java.io.InputStreamReader

@Composable
fun TextItemPreview(item: MItem) {
    val context = LocalContext.current
    val preview = remember(item.uri) {
        try {
            context.contentResolver.openInputStream(item.uri)
                ?.bufferedReader()?.use {
                    it.readText().take(200) + "..."
                } ?: "Немає попереднього перегляду"
        } catch (e: Exception) {
            "Помилка завантаження"
        }
    }
    Text(
        text = preview,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 5,
        overflow = TextOverflow.Ellipsis,
        color = Color.White.copy(0.8f),
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
            .border(
                width = 2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                    )
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    )
}