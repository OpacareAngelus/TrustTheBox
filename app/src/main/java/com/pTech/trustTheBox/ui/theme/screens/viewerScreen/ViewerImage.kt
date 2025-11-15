package com.pTech.trustTheBox.ui.theme.screens.viewerScreen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.pTech.trustTheBox.model.MItem

@Composable
fun ViewerImage(item: MItem) {
    AsyncImage(
        model = item.uri,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Fit
    )
}
