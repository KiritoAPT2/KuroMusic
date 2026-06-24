package com.kuromusic.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuromusic.innertube.YouTube
import com.kuromusic.innertube.models.ArtistItem
import com.kuromusic.innertube.models.SongItem
import com.kuromusic.innertube.models.WatchEndpoint
import com.kuromusic.innertube.models.YTItem
import com.kuromusic.innertube.pages.ExplorePage
import com.kuromusic.innertube.pages.HomePage
import com.kuromusic.innertube.utils.completedLibraryPage
import com.kuromusic.db.MusicDatabase
import com.kuromusic.db.entities.Artist
import com.kuromusic.db.entities.Playlist
import com.kuromusic.db.entities.Song
import com.kuromusic.constants.ShowAnimaxSectionKey
import com.kuromusic.utils.dataStore
import com.kuromusic.utils.get
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
    @ApplicationContext val appContext: Context,
    val database: MusicDatabase,
    val downloadUtil: DownloadUtil,
) : ViewModel() {
    val isRefreshing = MutableStateFlow(false)
    val isLoading = MutableStateFlow(false)

    val homePage = MutableStateFlow<HomePage?>(null)
    val explorePage = MutableStateFlow<ExplorePage?>(null)
    val recentActivity = MutableStateFlow<List<YTItem>?>(null)
    val recentPlaylistsDb = MutableStateFlow<List<Playlist>?>(null)
    val forYouMix = MutableStateFlow<List<YTItem>?>(null)

    val accountName = MutableStateFlow("Guest")
    val accountImageUrl = MutableStateFlow<String?>(null)

    val recentlyPlayed = database.historyDao.getRecentHistory(10)
    val essentialSongs = database.songDao.essentialSongs()
    val topArtist = database.songDao.topRecentArtist()
    val topGenre = database.songDao.topRecentGenre()
    val animaxVideos = MutableStateFlow<List<YTItem>?>(null)
    val globalRecommendations = MutableStateFlow<List<YTItem>>(emptyList())
    
    val localRecommendations = MutableStateFlow<List<Song>>(emptyList())
    
    // Dynamic gradient color extracted from album art
    val dynamicGradientColor = MutableStateFlow(androidx.compose.ui.graphics.Color(0xFF121212))

    private suspend fun load() = kotlinx.coroutines.withContext(Dispatchers.IO) {
        isLoading.value = true

        val showAnimax = appContext.dataStore.get(ShowAnimaxSectionKey, true)

        // **PARALLEL LOADING**: Launch all network requests concurrently
        val animaxDeferred = if (showAnimax) {
            async { loadAnimaxVideos() }
        } else {
            async { }
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
            val historyCount = database.statsDao.eventCount().first()
            if (historyCount < 5) {
                YouTube.browse("FEmusic_charts", null).onSuccess { result ->
                    globalRecommendations.value = result.items.flatMap { it.items }.take(15)
                }
            }
        }

        // Wait for all to complete
        awaitAll(animaxDeferred, homePageDeferred, explorePageDeferred, forYouMixDeferred, globalRecsDeferred)

        // Cargar recomendaciones DESPUÉS de que todos los datos están listos (evita race condition)
        if (!showAnimax) {
            loadAlgorithmicRecommendations()
        }

        if (homePage.value == null) {
            computeLocalRecommendations()
        }

        isLoading.value = false
    }

    private suspend fun computeLocalRecommendations() {
        val excludeIds = database.statsDao.getRecentPlayedIds(50).first()
        val genres = database.statsDao.getTopGenres(3).first()

        if (genres.isNotEmpty()) {
            val genreRecs = database.statsDao.getSongsFromGenres(genres, excludeIds, 20).first()
            var combined = genreRecs

            if (genreRecs.size < 10) {
                val exploration = database.statsDao.getUnplayedSongsFromGenres(genres, 15).first()
                combined = (genreRecs + exploration).distinctBy { it.id }.take(20)
            }

            if (combined.size < 5) {
                val essentials = database.songDao.essentialSongs(15).first()
                combined = (combined + essentials).distinctBy { it.id }.take(20)
            }

            localRecommendations.value = combined
        } else {
            val essentials = database.songDao.essentialSongs(20).first()
            localRecommendations.value = essentials
        }
    }

    private suspend fun loadAlgorithmicRecommendations() {
        // Tier 1: forYouMix (recomendaciones personalizadas)
        val mix = forYouMix.value
        if (!mix.isNullOrEmpty()) {
            animaxVideos.value = mix.shuffled().take(15)
            return
        }

        // Tier 2: globalRecommendations (charts o fallback desde homePage)
        var recs = globalRecommendations.value
        if (recs.isEmpty()) {
            val page = homePage.value
            if (page != null) {
                val items = page.sections.flatMap { it.items }.take(15)
                globalRecommendations.value = items
                recs = items
            }
        }
        if (recs.isNotEmpty()) {
            animaxVideos.value = recs.shuffled().take(15)
            return
        }

        // Tier 3 + super-fallback: cualquier item del homePage
        val home = homePage.value
        if (home != null) {
            val songs = home.sections.flatMap { it.items }.filterIsInstance<SongItem>()
            if (songs.isNotEmpty()) {
                animaxVideos.value = songs.shuffled().take(15)
                return
            }
            val anyItems = home.sections.flatMap { it.items }.take(15)
            if (anyItems.isNotEmpty()) {
                animaxVideos.value = anyItems
            }
        }
    }

    private suspend fun loadAnimaxVideos() {
        val animaxQuery = "Animax L Music"
        YouTube.search(animaxQuery, YouTube.SearchFilter.FILTER_ARTIST).onSuccess { page ->
             val channel = page.items.find { it is ArtistItem && it.title.equals("Animax L Music", ignoreCase = true) } as? ArtistItem
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
                             .filter { it.artists.any { a -> a.name.equals("Animax L Music", ignoreCase = true) } }
                             .sortedByDescending { 
                                 downloadUtil.downloads.value.containsKey(it.id) 
                             }.take(15)
                     }
                 }
             } else {
                 YouTube.search(animaxQuery, YouTube.SearchFilter.FILTER_VIDEO).onSuccess {
                     animaxVideos.value = it.items.filterIsInstance<SongItem>()
                         .filter { item -> item.artists.any { a -> a.name.equals("Animax L Music", ignoreCase = true) } }
                         .take(10)
                 }
             }
        }.onFailure {
             YouTube.search(animaxQuery, YouTube.SearchFilter.FILTER_VIDEO).onSuccess {
                 animaxVideos.value = it.items.filterIsInstance<SongItem>()
                    .filter { item -> item.artists.any { a -> a.name.equals("Animax L Music", ignoreCase = true) } }
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
            if (homePage.value == null) {
                computeLocalRecommendations()
            }
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
