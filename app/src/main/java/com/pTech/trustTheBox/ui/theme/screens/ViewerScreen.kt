package com.pTech.trustTheBox.ui.theme.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.rememberAsyncImagePainter
import com.pTech.trustTheBox.R
import com.pTech.trustTheBox.model.MItem
import com.pTech.trustTheBox.ui.theme.component.SetBackground

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun ViewerScreen(item: MItem) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize()) {
        SetBackground()
        if (item.type == "image") {
            Image(
                painter = rememberAsyncImagePainter(item.uri),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            val player = remember {
                ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(item.uri))
                    repeatMode = Player.REPEAT_MODE_ALL
                    prepare()
                }
            }
            AndroidView(
                factory = {
                    PlayerView(context).apply {
                        this.player = player
                        useController = true
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            DisposableEffect(Unit) {
                player.playWhenReady = true
                onDispose { player.release() }
            }
        }
    }
}