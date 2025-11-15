package com.pTech.trustTheBox.model

import android.net.Uri

data class MItem(
    val type: String,
    val uri: Uri,
    val thumbnailUri: Uri? = null,
    val isLandscape: Boolean = false,
    val previewText: String? = ""
)