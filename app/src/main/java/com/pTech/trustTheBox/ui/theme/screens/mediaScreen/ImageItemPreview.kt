package com.pTech.trustTheBox.ui.theme.screens.mediaScreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.rememberAsyncImagePainter
import com.pTech.trustTheBox.model.MItem

@Composable
fun ImageItemPreview(item: MItem) {
    Image(
        painter = rememberAsyncImagePainter(item.thumbnailUri ?: item.uri),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
}