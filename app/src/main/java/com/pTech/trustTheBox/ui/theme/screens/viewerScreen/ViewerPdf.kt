package com.pTech.trustTheBox.ui.theme.screens.viewerScreen

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.pTech.trustTheBox.model.MItem

@Composable
fun ViewerPdf(item: MItem) {
    val context = LocalContext.current
    val fd = remember { context.contentResolver.openFileDescriptor(item.uri, "r") }
    val renderer = remember(fd) { PdfRenderer(fd!!) }
    val pagerState = rememberPagerState(pageCount = { renderer.pageCount })
    VerticalPager(state = pagerState) { pageIndex ->
        val bitmap = remember(pageIndex) {
            val page = renderer.openPage(pageIndex)
            val bmp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888).apply {
                page.render(this, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            }
            page.close()
            bmp
        }
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
    DisposableEffect(Unit) {
        onDispose {
            renderer.close()
            fd?.close()
        }
    }
}