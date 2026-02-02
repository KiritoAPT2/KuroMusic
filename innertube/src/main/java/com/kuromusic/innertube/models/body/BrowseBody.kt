package com.kuromusic.innertube.models.body

import com.kuromusic.innertube.models.Context
import com.kuromusic.innertube.models.Continuation
import kotlinx.serialization.Serializable

@Serializable
data class BrowseBody(
    val context: Context,
    val browseId: String?,
    val params: String?,
    val continuation: String?
)
