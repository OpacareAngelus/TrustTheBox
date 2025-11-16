package com.pTech.trustTheBox.ui.theme.screens.mediaScreen

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.pTech.trustTheBox.model.MItem

@Composable
fun UnsupportedItemPreview(item: MItem) {
    Text(
        text = "Непідтримуваний формат",
        style = MaterialTheme.typography.bodySmall
    )
}