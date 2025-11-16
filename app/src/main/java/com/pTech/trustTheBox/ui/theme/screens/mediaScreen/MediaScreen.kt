package com.pTech.trustTheBox.ui.theme.screens.mediaScreen

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
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
                            .clickable { navController.navigate("viewer/${item.id}") },
                        contentAlignment = Alignment.Center
                    ) {
                        when (item.type) {
                            "image", "pdf" -> ImageItemPreview(item)
                            "video" -> VideoItemPreview(item)
                            "text", "document" -> TextItemPreview(item)
                            else -> UnsupportedItemPreview(item)
                        }
                    }
                }
            }
        }
    }
}
