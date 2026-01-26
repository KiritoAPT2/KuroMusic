package com.kuromusic.models

import com.kuromusic.innertube.models.YTItem
import com.kuromusic.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
