package com.pTech.trustTheBox.ui.theme.screens.mediaScreen

import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import coil3.compose.rememberAsyncImagePainter
import com.pTech.trustTheBox.R
import com.pTech.trustTheBox.model.MItem
import com.pTech.trustTheBox.ui.theme.component.SetBackground

@UnstableApi
@Composable
fun MediaScreen(
    mediaFiles: List<MItem>,
    navController: NavController
) {
    Box(modifier = Modifier.fillMaxSize()) {
        SetBackground()
        val sorted = mediaFiles.sortedByDescending { it.isLandscape }
        if (sorted.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(
                    stringResource(R.string.no_media_files),
                    style = MaterialTheme.typography.headlineLarge
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(sorted.size) { i ->
                    val item = sorted[i]
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = 2.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.secondary,
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                                    )
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { navController.navigate("viewer/$i") },
                        contentAlignment = Alignment.Center
                    ) {
                        when (item.type) {
                            "image", "pdf" -> {
                                Image(
                                    painter = rememberAsyncImagePainter(item.thumbnailUri ?: item.uri),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            "video" -> {
                                val context = LocalContext.current
                                val exoPlayer = remember(item.uri) {
                                    ExoPlayer.Builder(context).build().apply {
                                        setMediaItem(MediaItem.fromUri(item.uri))
                                        prepare()
                                        repeatMode = Player.REPEAT_MODE_ALL
                                        volume = 0f
                                        playWhenReady = true
                                    }
                                }

                                DisposableEffect(Unit) {
                                    onDispose {
                                        exoPlayer.release()
                                    }
                                }

                                AndroidView(
                                    factory = { ctx ->
                                        PlayerView(ctx).apply {
                                            player = exoPlayer
                                            layoutParams = ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                            useController = false
                                            setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            "text", "document" -> {
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
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp)
                                )
                            }
                            else -> {
                                Text(
                                    text = "Непідтримуваний формат",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}