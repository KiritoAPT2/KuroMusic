package com.grp2.kuromusic.models

import com.arturo254.innertube.models.YTItem
import com.grp2.kuromusic.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
