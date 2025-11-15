package com.pTech.trustTheBox.ui.theme.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pTech.trustTheBox.R

@Composable
fun SetBackground(){
    Image(
        painter = painterResource(id = R.drawable.main_background),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.8f)
            .blur(radius = 8.dp)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Black.copy(alpha = 0.25f))
    )
}