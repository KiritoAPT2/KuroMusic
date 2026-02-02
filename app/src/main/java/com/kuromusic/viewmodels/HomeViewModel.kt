package com.kuromusic.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuromusic.innertube.YouTube
import com.kuromusic.innertube.models.ArtistItem
import com.kuromusic.innertube.models.PlaylistItem
import com.kuromusic.innertube.models.SongItem
import com.kuromusic.innertube.models.WatchEndpoint
import com.kuromusic.innertube.models.YTItem
import com.kuromusic.innertube.pages.ExplorePage
import com.kuromusic.innertube.pages.HomePage
import com.kuromusic.innertube.utils.completedLibraryPage
import com.kuromusic.db.MusicDatabase
import com.kuromusic.db.entities.Album
import com.kuromusic.db.entities.Artist
import com.kuromusic.db.entities.LocalItem
import com.kuromusic.db.entities.Playlist
import com.kuromusic.db.entities.Song
import com.kuromusic.models.SimilarRecommendation
import com.kuromusic.utils.reportException
import com.kuromusic.playback.DownloadUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext context: Context,
    val database: MusicDatabase,
    val downloadUtil: DownloadUtil,
) : ViewModel() {
    val isRefreshing = MutableStateFlow(false)
    val isLoading = MutableStateFlow(false)

    val quickPicks = MutableStateFlow<List<Song>?>(null)
    val forgottenFavorites = MutableStateFlow<List<Song>?>(null)
    val keepListening = MutableStateFlow<List<LocalItem>?>(null)
    val similarRecommendations = MutableStateFlow<List<SimilarRecommendation>?>(null)
    val accountPlaylists = MutableStateFlow<List<PlaylistItem>?>(null)
    val homePage = MutableStateFlow<HomePage?>(null)
    val explorePage = MutableStateFlow<ExplorePage?>(null)
    val recentActivity = MutableStateFlow<List<YTItem>?>(null)
    val recentPlaylistsDb = MutableStateFlow<List<Playlist>?>(null)

    val allLocalItems = MutableStateFlow<List<LocalItem>>(emptyList())
    val forYouMix = MutableStateFlow<List<YTItem>?>(null)

    val accountName = MutableStateFlow("Guest")
    val accountImageUrl = MutableStateFlow<String?>(null)

    val recentlyPlayed = database.getRecentHistory(10)
    val essentialSongs = database.essentialSongs()
    val topArtist = database.topRecentArtist()
    val topGenre = database.topRecentGenre()
    val animaxVideos = MutableStateFlow<List<YTItem>?>(null)
    val globalRecommendations = MutableStateFlow<List<YTItem>>(emptyList())
    
    // Dynamic gradient color extracted from album art
    val dynamicGradientColor = MutableStateFlow(androidx.compose.ui.graphics.Color(0xFF121212))

    private suspend fun load() = kotlinx.coroutines.withContext(Dispatchers.IO) {
        isLoading.value = true

        // **PARALLEL LOADING**: Launch all network requests concurrently for 3-4x speed improvement
        val animaxDeferred = async {
            loadAnimaxVideos()
        }
        
        val homePageDeferred = async {
            YouTube.home().onSuccess { page ->
                val filteredSections = page.sections.filter { section ->
                    !section.title.contains("Animax", ignoreCase = true)
                }
                homePage.value = page.copy(sections = filteredSections)
            }.onFailure {
                reportException(it)
            }
        }
        
        val explorePageDeferred = async {
            YouTube.explore().onSuccess { page ->
                explorePage.value = page
            }.onFailure {
                reportException(it)
            }
        }
        
        val forYouMixDeferred = async {
            YouTube.browse("FEmusic_home", "w6lsAUIB").onSuccess { result ->
                forYouMix.value = result.items.flatMap { it.items }
            }
        }
        
        val globalRecsDeferred = async {
            val historyCount = database.eventCount().first()
            if (historyCount < 5) {
                YouTube.browse("FEmusic_charts", null).onSuccess { result ->
                    globalRecommendations.value = result.items.flatMap { it.items }.take(15)
                }.onFailure {
                    globalRecommendations.value = homePage.value?.sections?.flatMap { it.items }?.take(15) ?: emptyList()
                }
            }
        }

        // Wait for all to complete
        awaitAll(animaxDeferred, homePageDeferred, explorePageDeferred, forYouMixDeferred, globalRecsDeferred)

        isLoading.value = false
    }

    private suspend fun loadAnimaxVideos() {
        val animaxQuery = "Animax L Music"
        YouTube.search(animaxQuery, YouTube.SearchFilter.FILTER_ARTIST).onSuccess { page ->
             val channel = page.items.find { it is ArtistItem && it.title.contains("Animax", true) } as? ArtistItem
             if (channel != null) {
                 YouTube.artist(channel.id).onSuccess { channelPage ->
                     val items = channelPage.sections
                         .filter { it.title?.contains("Video", true) == true || it.title?.contains("Canciones", true) == true }
                         .flatMap { it.items }
                         .filterIsInstance<SongItem>()
                     
                     if (items.isNotEmpty()) {
                         animaxVideos.value = items.sortedByDescending { 
                             downloadUtil.downloads.value.containsKey(it.id) 
                         }.take(15)
                     } else {
                         animaxVideos.value = channelPage.sections.flatMap { it.items }
                             .filterIsInstance<SongItem>()
                             .filter { it.artists.any { a -> a.name.contains("Animax", true) } }
                             .sortedByDescending { 
                                 downloadUtil.downloads.value.containsKey(it.id) 
                             }.take(15)
                     }
                 }
             } else {
                 YouTube.search(animaxQuery, YouTube.SearchFilter.FILTER_VIDEO).onSuccess {
                     animaxVideos.value = it.items.filterIsInstance<SongItem>()
                         .filter { item -> item.artists.any { a -> a.name.contains("Animax", true) } }
                         .take(10)
                 }
             }
        }.onFailure {
             YouTube.search(animaxQuery, YouTube.SearchFilter.FILTER_VIDEO).onSuccess {
                 animaxVideos.value = it.items.filterIsInstance<SongItem>()
                    .filter { item -> item.artists.any { a -> a.name.contains("Animax", true) } }
                    .take(10)
             }
             reportException(it)
        }
    }

    private var lastRefreshTime = 0L
    
    fun refresh() {
        // Debounce: prevent rapid-fire refresh calls
        val currentTime = System.currentTimeMillis()
        if (isRefreshing.value || (currentTime - lastRefreshTime) < 300) return
        
        lastRefreshTime = currentTime
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing.value = true
            load()
            isRefreshing.value = false
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            load()
        }
        // Room Flow handles recentlyPlayed updates automatically
        // No need to reload entire page on each song change
    }
}
