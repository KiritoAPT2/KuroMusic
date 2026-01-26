package com.kuromusic.innertube.pages

import com.kuromusic.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
