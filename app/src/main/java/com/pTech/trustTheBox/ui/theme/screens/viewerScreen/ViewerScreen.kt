package com.pTech.trustTheBox.ui.theme.screens.viewerScreen

import ViewerText
import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pTech.trustTheBox.model.MItem
import com.pTech.trustTheBox.ui.theme.component.SetBackground

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun ViewerScreen(item: MItem) {
    Box(modifier = Modifier.fillMaxSize()) {
        SetBackground()
        when (item.type) {
            "image" -> {
                ViewerImage(item)
            }

            "video" -> {
                ViewerVideo(item)
            }

            "text" -> {
               ViewerText(item)
            }

            "pdf" -> {
                ViewerPdf(item)
            }

            "document" -> {
          ViewerDocument(item)
            }

            else -> {
                Text(
                    text = "Непідтримуваний формат файлу",
                    modifier = Modifier.fillMaxSize(),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}





