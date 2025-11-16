package com.pTech.trustTheBox.model

import android.net.Uri

data class MItem(
    val id: Long,
    val type: String,
    val uri: Uri,
    val thumbnailUri: Uri? = null,
    val isLandscape: Boolean = false,
    val previewText: String? = ""
)