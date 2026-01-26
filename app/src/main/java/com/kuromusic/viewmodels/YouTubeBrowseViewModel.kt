package com.kuromusic.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuromusic.innertube.YouTube
import com.kuromusic.innertube.pages.BrowseResult
import com.kuromusic.constants.HideExplicitKey
import com.kuromusic.utils.dataStore
import com.kuromusic.utils.get
import com.kuromusic.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class YouTubeBrowseViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val browseId = savedStateHandle.get<String>("browseId")!!
    private val params = savedStateHandle.get<String>("params")

    val result = MutableStateFlow<BrowseResult?>(null)

    init {
        viewModelScope.launch {
            YouTube
                .browse(browseId, params)
                .onSuccess {
                    result.value = it.filterExplicit(context.dataStore.get(HideExplicitKey, false))
                }.onFailure {
                    reportException(it)
                }
        }
    }
}
