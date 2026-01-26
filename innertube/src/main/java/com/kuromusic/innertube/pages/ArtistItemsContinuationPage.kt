package com.kuromusic.innertube.pages

import com.kuromusic.innertube.models.YTItem

data class ArtistItemsContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
