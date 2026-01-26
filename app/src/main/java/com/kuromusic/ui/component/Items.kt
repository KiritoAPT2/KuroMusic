@file:OptIn(ExperimentalFoundationApi::class)

package com.kuromusic.ui.component

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.Download.STATE_COMPLETED
import androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING
import androidx.media3.exoplayer.offline.Download.STATE_QUEUED
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import com.kuromusic.LocalDatabase
import com.kuromusic.LocalDownloadUtil
import com.kuromusic.LocalPlayerConnection
import com.kuromusic.R
import com.kuromusic.constants.GridThumbnailHeight
import com.kuromusic.constants.ListItemHeight
import com.kuromusic.constants.ListThumbnailSize
import com.kuromusic.constants.SmallGridThumbnailHeight
import com.kuromusic.constants.ThumbnailCornerRadius
import com.kuromusic.db.entities.Album
import com.kuromusic.db.entities.Artist
import com.kuromusic.db.entities.Playlist
import com.kuromusic.db.entities.Song
import com.kuromusic.innertube.YouTube
import com.kuromusic.innertube.models.AlbumItem
import com.kuromusic.innertube.models.ArtistItem
import com.kuromusic.innertube.models.PlaylistItem
import com.kuromusic.innertube.models.SongItem
import com.kuromusic.innertube.models.YTItem
import com.kuromusic.models.MediaMetadata
import com.kuromusic.playback.queues.LocalAlbumRadio
import com.kuromusic.ui.menu.AlbumMenu
import com.kuromusic.ui.menu.ArtistMenu
import com.kuromusic.ui.menu.SongMenu
import com.kuromusic.ui.theme.extractThemeColor
import com.kuromusic.utils.getPlaylistImageUri
import com.kuromusic.utils.joinByBullet
import com.kuromusic.utils.makeTimeString
import com.kuromusic.utils.reportException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.fadingEdge(brush: Brush) = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(brush = brush, blendMode = BlendMode.DstIn)
    }

@Composable
inline fun ListItem(
    modifier: Modifier = Modifier,
    title: String,
    noinline subtitle: (@Composable RowScope.() -> Unit)? = null,
    thumbnailContent: @Composable () -> Unit,
    trailingContent: @Composable RowScope.() -> Unit = {},
    isActive: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            if (isActive) {
                modifier
                    .height(ListItemHeight)
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color = MaterialTheme.colorScheme.secondaryContainer)
            } else {
                modifier
                    .height(ListItemHeight)
                    .padding(horizontal = 8.dp)
            },
    ) {
        Box(
            modifier = Modifier.padding(6.dp),
            contentAlignment = Alignment.Center,
        ) {
            thumbnailContent()
        }
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp),
        ) {
            DisableSelection {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier
                            .basicMarquee()
                            .fillMaxWidth(),
                )
            }

            if (subtitle != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    subtitle()
                }
            }
        }

        trailingContent()
    }
}

@Composable
fun ListItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String?,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailContent: @Composable () -> Unit,
    trailingContent: @Composable RowScope.() -> Unit = {},
    isActive: Boolean = false,
) = ListItem(
    title = title,
    subtitle = {
        badges()

        if (!subtitle.isNullOrEmpty()) {
            DisableSelection {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    },
    thumbnailContent = thumbnailContent,
    trailingContent = trailingContent,
    modifier = modifier,
    isActive = isActive,
)

@Composable
fun GridItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailContent: @Composable BoxWithConstraintsScope.() -> Unit,
    thumbnailShape: Shape,
    thumbnailRatio: Float = 1f,
    fillMaxWidth: Boolean = false,
) {
    Column(
        modifier =
            if (fillMaxWidth) {
                modifier
                    .padding(12.dp)
                    .fillMaxWidth()
            } else {
                modifier
                    .padding(12.dp)
                    .width(GridThumbnailHeight * thumbnailRatio)

            },
    ) {
        BoxWithConstraints(
            contentAlignment =
                Alignment.Center,
            modifier =
                if (fillMaxWidth) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.height(GridThumbnailHeight)
                }
                    .aspectRatio(thumbnailRatio)
                    .clip(RoundedCornerShape(27.dp)),
        ) {
            thumbnailContent()
        }

        Spacer(modifier = Modifier.height(6.dp))

        DisableSelection {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
                modifier =
                    Modifier
                        .basicMarquee()
                        .fillMaxWidth(),
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            badges()

            DisableSelection {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun SmallGridItem(
    modifier: Modifier = Modifier,
    title: String,
    thumbnailContent: @Composable BoxWithConstraintsScope.() -> Unit,
    thumbnailShape: Shape,
    thumbnailRatio: Float = 1f,
    isArtist: Boolean? = false,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = if (isArtist == true) Alignment.CenterHorizontally else Alignment.Start,
        modifier =
            modifier
                .fillMaxHeight()
                .width(GridThumbnailHeight * thumbnailRatio)
                .padding(12.dp),
    ) {
        BoxWithConstraints(
            modifier =
                Modifier
                    .height(SmallGridThumbnailHeight)
                    .aspectRatio(thumbnailRatio)
                    .clip(RoundedCornerShape(8.dp))
        ) {
            thumbnailContent()
        }

        Spacer(modifier = Modifier.height(6.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.width(SmallGridThumbnailHeight),
        ) {
            DisableSelection {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                    modifier =
                        Modifier
                            .basicMarquee()
                            .fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun SongListItem(
    song: Song,
    modifier: Modifier = Modifier,
    albumIndex: Int? = null,
    showLikedIcon: Boolean = true,
    showInLibraryIcon: Boolean = false,
    showDownloadIcon: Boolean = true,
    isSelected: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    badges: @Composable RowScope.() -> Unit = {
        if (showLikedIcon && song.song.liked) {
            Icon(
                painter = painterResource(R.drawable.favorite),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }
        if (showInLibraryIcon && song.song.inLibrary != null) {
            Icon(
                painter = painterResource(R.drawable.library_add_check),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }
        if (showDownloadIcon) {
            val download by LocalDownloadUtil.current.getDownload(song.id)
                .collectAsState(initial = null)
            when (download?.state) {
                STATE_COMPLETED ->
                    Icon(
                        painter = painterResource(R.drawable.offline),
                        contentDescription = null,
                        modifier =
                            Modifier
                                .size(18.dp)
                                .padding(end = 2.dp),
                    )

                STATE_QUEUED, STATE_DOWNLOADING ->
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier =
                            Modifier
                                .size(16.dp)
                                .padding(end = 2.dp),
                    )

                else -> {}
            }
        }
    },
    trailingContent: @Composable RowScope.() -> Unit = {},
    inSelectionMode: Boolean = false,
    onSelectionChange: ((Boolean) -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val songSubtitle = remember(song) {
        joinByBullet(
            song.artists.joinToString { it.name },
            makeTimeString(song.song.duration * 1000L)
        )
    }
    ListItem(
        title = song.song.title,
        subtitle = songSubtitle,
        badges = badges,
        thumbnailContent = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(ListThumbnailSize),
            ) {
                if (albumIndex != null) {
                    AnimatedVisibility(
                        visible = !isActive,
                        enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
                        exit = shrinkOut(shrinkTowards = Alignment.Center) + fadeOut(),
                    ) {
                        if (isSelected) {
                            Icon(
                                painter = painterResource(R.drawable.done),
                                modifier = Modifier.align(Alignment.Center),
                                contentDescription = null,
                            )
                        } else {
                            Text(
                                text = albumIndex.toString(),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                } else {
                    if (isSelected) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .zIndex(1000f)
                                    .clip(RoundedCornerShape(ThumbnailCornerRadius))
                                    .background(Color.Black.copy(alpha = 0.5f)),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.done),
                                modifier = Modifier.align(Alignment.Center),
                                contentDescription = null,
                            )
                        }
                    }
                    var thumbnailColor by remember { mutableStateOf(Color.Transparent) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(ThumbnailCornerRadius))
                            .background(thumbnailColor.copy(alpha = 0.2f))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(song.song.thumbnailUrl)
                                .crossfade(true)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .size(150, 150) // Reduced to 150x150 to reduce GC pressure (Mali fix)
                                .precision(coil.size.Precision.EXACT)
                                .allowHardware(false) // Necesario para poder convertir a Bitmap y extraer color
                                .build(),
                            onSuccess = { state ->
                                // Solo lanzamos la extracción si el color es transparente (no calculado aún)
                                if (thumbnailColor == Color.Transparent) {
                                    scope.launch(Dispatchers.Default) {
                                        state.result.drawable.toBitmapOrNull()?.let { bitmap ->
                                            val color = bitmap.extractThemeColor()
                                            withContext(Dispatchers.Main) {
                                                thumbnailColor = color
                                            }
                                        }
                                    }
                                }
                            },
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        
                        // Scrim
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)),
                                        startY = 0.6f
                                    )
                                )
                        )
                    }
                }

                PlayingIndicatorBox(
                    isActive = isActive,
                    playWhenReady = isPlaying,
                    color = if (albumIndex != null) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                color =
                                    if (albumIndex != null) {
                                        Color.Transparent
                                    } else {
                                        Color.Black.copy(
                                            alpha = 0.4f,
                                        )
                                    },
                                shape = RoundedCornerShape(ThumbnailCornerRadius),
                            ),
                )
            }
        },
        trailingContent = trailingContent,
        modifier = modifier,
        isActive = isActive,
    )
}

@Composable
fun SongGridItem(
    song: Song,
    modifier: Modifier = Modifier,
    showLikedIcon: Boolean = true,

    showInLibraryIcon: Boolean = false,
    showDownloadIcon: Boolean = true,
    badges: @Composable RowScope.() -> Unit = {
        if (showLikedIcon && song.song.liked) {
            Icon(
                painter = painterResource(R.drawable.favorite),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .size(18.dp)
                    .padding(end = 2.dp)
            )
        }
        if (showInLibraryIcon && song.song.inLibrary != null) {
            Icon(
                painter = painterResource(R.drawable.library_add_check),
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .padding(end = 2.dp)
            )
        }
        if (showDownloadIcon) {
            val download by LocalDownloadUtil.current.getDownload(song.id)
                .collectAsState(initial = null)
            when (download?.state) {
                STATE_COMPLETED -> Icon(
                    painter = painterResource(R.drawable.offline),
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(end = 2.dp)
                )

                STATE_QUEUED, STATE_DOWNLOADING -> CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(end = 2.dp)
                )

                else -> {}
            }
        }
    },
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
) {
    val songSubtitle = remember(song) {
        joinByBullet(
            song.artists.joinToString { it.name },
            makeTimeString(song.song.duration * 1000L)
        )
    }
    GridItem(
        title = song.song.title,
        subtitle = songSubtitle,
        badges = badges,
        thumbnailContent = {
            var thumbnailColor by remember(song.song.thumbnailUrl) { mutableStateOf(Color.Transparent) }
            val scope = rememberCoroutineScope()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(ThumbnailCornerRadius))
                    .background(thumbnailColor.copy(alpha = 0.15f))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(song.song.thumbnailUrl)
                        .crossfade(true)
                        .allowHardware(false) // Mantenlo para Palette
                        .diskCachePolicy(CachePolicy.ENABLED) // Forzar uso de disco
                        .memoryCachePolicy(CachePolicy.ENABLED) // Forzar uso de RAM
                        .size(150, 150) // Tamaño fijo para el decoder
                        .build(),
                onSuccess = { state ->
                    // Solo extraer si el color es transparente
                if (thumbnailColor == Color.Transparent) {
                    // Mover a Default para no bloquear el Main Thread de los logs
                    scope.launch(Dispatchers.Default) {
                        // Use standard toBitmap from AndroidX KTX
                        state.result.drawable.toBitmap(50, 50).let { bitmap ->
                            val color = bitmap.extractThemeColor()
                            withContext(Dispatchers.Main) {
                                thumbnailColor = color
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            contentDescription = null,
        )
            
            // Scrim for better contrast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
                            startY = 0.5f
                        )
                    )
            )

            AnimatedVisibility(
                visible = isActive,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(500)),
                modifier =
                    Modifier
                        .align(Alignment.Center),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                color = Color.Black.copy(alpha = if (isPlaying) 0.4f else 0f),
                                shape = RoundedCornerShape(ThumbnailCornerRadius),
                            ),
                ) {
                    if (isPlaying) {
                        PlayingIndicator(
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.height(24.dp),
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !(isActive && isPlaying),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .padding(8.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f)),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    },
    thumbnailShape = RoundedCornerShape(ThumbnailCornerRadius),
    fillMaxWidth = fillMaxWidth,
    modifier = modifier
    )
}

@Composable
fun SongSmallGridItem(
    song: Song,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
) = SmallGridItem(
    title = song.song.title,
    thumbnailContent = {
        var thumbnailColor by remember { mutableStateOf(Color.Transparent) }
        val scope = rememberCoroutineScope()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(ThumbnailCornerRadius))
                .background(thumbnailColor.copy(alpha = 0.2f))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.song.thumbnailUrl)
                    .crossfade(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .size(150, 150) // Limit size for small grid items (Mali fix)
                    .precision(coil.size.Precision.INEXACT)
                    .allowHardware(false)
                    .build(),
                onSuccess = { state ->
                    if (thumbnailColor == Color.Transparent) {
                        scope.launch(Dispatchers.Default) {
                            state.result.drawable.toBitmapOrNull()?.let { bitmap ->
                                val color = bitmap.extractThemeColor()
                                withContext(Dispatchers.Main) {
                                    thumbnailColor = color
                                }
                            }
                        }
                    }
                },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            
            // Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)),
                            startY = 0.6f
                        )
                    )
            )

            AnimatedVisibility(
                visible = isActive,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(500)),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                color = Color.Black.copy(alpha = if (isPlaying) 0.4f else 0f),
                                shape = RoundedCornerShape(ThumbnailCornerRadius),
                            ),
                ) {
                    if (isPlaying) {
                        PlayingIndicator(
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.height(24.dp),
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !(isActive && isPlaying),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .padding(8.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f)),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    },
    thumbnailShape = RoundedCornerShape(ThumbnailCornerRadius),
    modifier = modifier,
)

@Composable
fun ArtistListItem(
    artist: Artist,
    modifier: Modifier = Modifier,
    badges: @Composable RowScope.() -> Unit = {
        if (artist.artist.bookmarkedAt != null) {
            Icon(
                painter = painterResource(R.drawable.favorite),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }
    },
    trailingContent: @Composable RowScope.() -> Unit = {},
) = ListItem(
    title = artist.artist.name,
    subtitle = pluralStringResource(R.plurals.n_song, artist.songCount, artist.songCount),
    badges = badges,
    thumbnailContent = {
        AsyncImage(
            model = artist.artist.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .size(ListThumbnailSize)
                    .clip(CircleShape),
        )
    },
    trailingContent = trailingContent,
    modifier = modifier,
)

@Composable
fun ArtistGridItem(
    artist: Artist,
    modifier: Modifier = Modifier,
    badges: @Composable RowScope.() -> Unit = {
        if (artist.artist.bookmarkedAt != null) {
            Icon(
                painter = painterResource(R.drawable.favorite),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }
    },
    fillMaxWidth: Boolean = false,
) = GridItem(
    title = artist.artist.name,
    subtitle = pluralStringResource(R.plurals.n_song, artist.songCount, artist.songCount),
    badges = badges,
    thumbnailContent = {
        AsyncImage(
            model = artist.artist.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            )
    },
    thumbnailShape = CircleShape,
    fillMaxWidth = fillMaxWidth,
    modifier = modifier,
)

@Composable
fun ArtistSmallGridItem(
    artist: Artist,
    modifier: Modifier = Modifier,
) = SmallGridItem(
    title = artist.artist.name,
    thumbnailContent = {
        AsyncImage(
            model = artist.artist.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    },
    thumbnailShape = CircleShape,
    modifier = modifier,
    isArtist = true,
)

@Composable
fun AlbumListItem(
    album: Album,
    modifier: Modifier = Modifier,
    showLikedIcon: Boolean = true,
    badges: @Composable RowScope.() -> Unit = {
        val database = LocalDatabase.current
        val downloadUtil = LocalDownloadUtil.current
        var songs by remember {
            mutableStateOf(emptyList<Song>())
        }

        LaunchedEffect(Unit) {
            database.albumSongs(album.id).collect {
                songs = it
            }
        }

        var downloadState by remember {
            mutableStateOf(Download.STATE_STOPPED)
        }

        LaunchedEffect(songs) {
            if (songs.isEmpty()) return@LaunchedEffect
            downloadUtil.downloads.collect { downloads ->
                downloadState =
                    if (songs.all { downloads[it.id]?.state == STATE_COMPLETED }) {
                        STATE_COMPLETED
                    } else if (songs.all {
                            downloads[it.id]?.state == STATE_QUEUED ||
                                    downloads[it.id]?.state == STATE_DOWNLOADING ||
                                    downloads[it.id]?.state == STATE_COMPLETED
                        }
                    ) {
                        STATE_DOWNLOADING
                    } else {
                        Download.STATE_STOPPED
                    }
            }
        }

        if (showLikedIcon && album.album.bookmarkedAt != null) {
            Icon(
                painter = painterResource(R.drawable.favorite),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }

        when (downloadState) {
            STATE_COMPLETED ->
                Icon(
                    painter = painterResource(R.drawable.offline),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(18.dp)
                            .padding(end = 2.dp),
                )

            STATE_DOWNLOADING ->
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier =
                        Modifier
                            .size(16.dp)
                            .padding(end = 2.dp),
                )

            else -> {}
        }
    },
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val songCountText = pluralStringResource(R.plurals.n_song, album.album.songCount, album.album.songCount)
    val subtitleText = remember(album, songCountText) {
        joinByBullet(
            album.artists.joinToString { it.name },
            songCountText,
            album.album.year?.toString()
        )
    }
    ListItem(
        title = album.album.title,
        subtitle = subtitleText,
        badges = badges,
        thumbnailContent = {
            val database = LocalDatabase.current
            val coroutineScope = rememberCoroutineScope()

            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalContext.current)
                        .data(album.album.thumbnailUrl)
                        .allowHardware(false)
                        .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onState = { state ->
                    if (album.album.themeColor == null && state is AsyncImagePainter.State.Success) {
                        coroutineScope.launch(Dispatchers.IO) {
                            state.result.drawable.toBitmapOrNull()?.extractThemeColor()?.toArgb()
                                ?.let { color ->
                                    database.query {
                                        update(album.album.copy(themeColor = color))
                                    }
                                }
                        }
                    }
                },
                modifier =
                    Modifier
                        .size(ListThumbnailSize)
                        .clip(RoundedCornerShape(ThumbnailCornerRadius)),
            )

            PlayingIndicatorBox(
                isActive = isActive,
                playWhenReady = isPlaying,
                modifier =
                    Modifier
                        .size(ListThumbnailSize)
                        .background(
                            color = Color.Black.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(ThumbnailCornerRadius),
                        ),
            )
        },
        trailingContent = trailingContent,
        modifier = modifier,
        isActive = isActive,
    )
}

@Composable
fun AlbumGridItem(
    album: Album,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope,
    badges: @Composable RowScope.() -> Unit = {
        val database = LocalDatabase.current
        val downloadUtil = LocalDownloadUtil.current
        var songs by remember {
            mutableStateOf(emptyList<Song>())
        }

        LaunchedEffect(Unit) {
            database.albumSongs(album.id).collect {
                songs = it
            }
        }

        var downloadState by remember {
            mutableStateOf(Download.STATE_STOPPED)
        }

        LaunchedEffect(songs) {
            if (songs.isEmpty()) return@LaunchedEffect
            downloadUtil.downloads.collect { downloads ->
                downloadState =
                    if (songs.all { downloads[it.id]?.state == STATE_COMPLETED }) {
                        STATE_COMPLETED
                    } else if (songs.all {
                            downloads[it.id]?.state == STATE_QUEUED ||
                                    downloads[it.id]?.state == STATE_DOWNLOADING ||
                                    downloads[it.id]?.state == STATE_COMPLETED
                        }
                    ) {
                        STATE_DOWNLOADING
                    } else {
                        Download.STATE_STOPPED
                    }
            }
        }

        if (album.album.bookmarkedAt != null) {
            Icon(
                painter = painterResource(R.drawable.favorite),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }

        when (downloadState) {
            STATE_COMPLETED ->
                Icon(
                    painter = painterResource(R.drawable.offline),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(18.dp)
                            .padding(end = 2.dp),
                )

            STATE_DOWNLOADING ->
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier =
                        Modifier
                            .size(16.dp)
                            .padding(end = 2.dp),
                )

            else -> {}
        }
    },
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
) {
    val albumSubtitle = remember(album) {
        album.artists.joinToString { it.name }
    }
    GridItem(
        title = album.album.title,
        subtitle = albumSubtitle,
        badges = badges,
        thumbnailContent = {
            AsyncImage(
                model = album.album.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            AnimatedVisibility(
                visible = isActive,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(500)),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                color = Color.Black.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(ThumbnailCornerRadius),
                            ),
                ) {
                    if (isPlaying) {
                        PlayingIndicator(
                            color = Color.White,
                            modifier = Modifier.height(24.dp),
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !isActive,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
            ) {
                val database = LocalDatabase.current
                val playerConnection = LocalPlayerConnection.current ?: return@AnimatedVisibility
                val coroutineScope = rememberCoroutineScope()

                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable {
                                coroutineScope.launch {
                                    database.albumWithSongs(album.id).first()?.let { albumWithSongs ->
                                        playerConnection.playQueue(
                                            LocalAlbumRadio(albumWithSongs)
                                        )
                                    }
                                }
                            },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
            }
        },
        thumbnailShape = RoundedCornerShape(ThumbnailCornerRadius),
        fillMaxWidth = fillMaxWidth,
        modifier = modifier,
    )
}

@Composable
fun AlbumSmallGridItem(
    song: Song,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
) = song.song.albumName?.let {
    SmallGridItem(
        title = it,
        thumbnailContent = {
            AsyncImage(
                model = song.song.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            AnimatedVisibility(
                visible = isActive,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(500)),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                color = Color.Black.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(ThumbnailCornerRadius),
                            ),
                ) {
                    if (isPlaying) {
                        PlayingIndicator(
                            color = Color.White,
                            modifier = Modifier.height(24.dp),
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                }
            }
        },
        thumbnailShape = RoundedCornerShape(ThumbnailCornerRadius),
        modifier = modifier,
    )
}

@Composable
fun PlaylistListItem(
    playlist: Playlist,
    modifier: Modifier = Modifier,
    trailingContent: @Composable RowScope.() -> Unit = {},
    autoPlaylist: Boolean = false,
) = ListItem(
    title = playlist.playlist.name,
    subtitle = if (autoPlaylist) {
        ""
    } else {
        if (playlist.songCount == 0 && playlist.playlist.remoteSongCount != null) {
            pluralStringResource(
                R.plurals.n_song,
                playlist.playlist.remoteSongCount,
                playlist.playlist.remoteSongCount
            )
        } else {
            pluralStringResource(R.plurals.n_song, playlist.songCount, playlist.songCount)
        }
    },
    thumbnailContent = {
        val painter =
            when (playlist.playlist.name) {
                stringResource(R.string.liked) -> R.drawable.favorite_border
                stringResource(R.string.offline) -> R.drawable.offline
                stringResource(R.string.cached_playlist) -> R.drawable.cached
                else -> {
                    if (autoPlaylist) {
                        R.drawable.trending_up
                    } else {
                        R.drawable.queue_music

                    }
                }
            }
        when (playlist.thumbnails.size) {
            0 ->
                Box(
                    modifier =
                        Modifier
                            .size(ListThumbnailSize)
                            .clip(RoundedCornerShape(ThumbnailCornerRadius))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Icon(
                        painter = painterResource(painter),
                        contentDescription = null,
                        modifier = Modifier
                            .size(ListThumbnailSize / 2)
                            .align(Alignment.Center)
                    )
                }

            1 ->
                AsyncImage(
                    model = playlist.thumbnails[0],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .size(ListThumbnailSize)
                            .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                )

            else ->
                Box(
                    modifier =
                        Modifier
                            .size(ListThumbnailSize)
                            .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                ) {
                    listOf(
                        Alignment.TopStart,
                        Alignment.TopEnd,
                        Alignment.BottomStart,
                        Alignment.BottomEnd,
                    ).fastForEachIndexed { index, alignment ->
                        AsyncImage(
                            model = playlist.thumbnails.getOrNull(index),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier =
                                Modifier
                                    .align(alignment)
                                    .size(ListThumbnailSize / 2),
                        )
                    }
                }
        }
    },
    trailingContent = trailingContent,
    modifier = modifier,
)

@Composable
fun PlaylistGridItem(
    playlist: Playlist,
    modifier: Modifier = Modifier,
    badges: @Composable RowScope.() -> Unit = { },
    fillMaxWidth: Boolean = false,
    autoPlaylist: Boolean = false,
    context: Context // Agregamos el contexto para obtener la URI de la imagen
) = GridItem(
    title = playlist.playlist.name,
    subtitle = if (autoPlaylist) {
        ""
    } else {
        if (playlist.songCount == 0 && playlist.playlist.remoteSongCount != null) {
            pluralStringResource(
                R.plurals.n_song,
                playlist.playlist.remoteSongCount,
                playlist.playlist.remoteSongCount
            )
        } else {
            pluralStringResource(R.plurals.n_song, playlist.songCount, playlist.songCount)
        }
    },
    badges = badges,
    thumbnailContent = {
        val thumbnailUri =
            getPlaylistImageUri(context, playlist.playlist.id) // Obtener URI de la miniatura

        if (thumbnailUri != null) {
            // Si la URI de la imagen existe, la mostramos
            AsyncImage(
                model = thumbnailUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(ThumbnailCornerRadius)),
            )
        } else {
            // Si no hay miniatura, mostrar la imagen predeterminada
            val painter =
                when (playlist.playlist.name) {
                    stringResource(R.string.liked) -> R.drawable.favorite_border
                    stringResource(R.string.offline) -> R.drawable.offline
                    stringResource(R.string.cached_playlist) -> R.drawable.cached
                    else -> {
                        if (autoPlaylist) {
                            R.drawable.trending_up
                        } else {
                            R.drawable.queue_music
                        }
                    }
                }
            val width = maxWidth
            val Libcarditem = 25.dp
            when (playlist.thumbnails.size) {
                0 ->
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .clip(RoundedCornerShape(Libcarditem))
                    ) {
                        Icon(
                            painter = painterResource(painter),
                            contentDescription = null,
                            tint = LocalContentColor.current.copy(alpha = 0.8f),
                            modifier =
                                Modifier
                                    .size(width / 2)
                                    .align(Alignment.Center),
                        )
                    }

                1 ->
                    AsyncImage(
                        model = playlist.thumbnails[0],
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                    )

                else ->
                    Box(
                        modifier =
                            Modifier
                                .size(width)
                                .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                    ) {
                        listOf(
                            Alignment.TopStart,
                            Alignment.TopEnd,
                            Alignment.BottomStart,
                            Alignment.BottomEnd,
                        ).fastForEachIndexed { index, alignment ->
                            AsyncImage(
                                model = playlist.thumbnails.getOrNull(index),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier =
                                    Modifier
                                        .align(alignment)
                                        .size(width / 2),
                            )
                        }
                    }
            }
        }
    },
    thumbnailShape = RoundedCornerShape(ThumbnailCornerRadius),
    fillMaxWidth = fillMaxWidth,
    modifier = modifier,
)


@Composable
fun MediaMetadataListItem(
    mediaMetadata: MediaMetadata,
    modifier: Modifier,
    isSelected: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val mediaSubtitle = remember(mediaMetadata) {
        joinByBullet(
            mediaMetadata.artists.joinToString { it.name },
            makeTimeString(mediaMetadata.duration * 1000L),
        )
    }
    ListItem(
        title = mediaMetadata.title,
        subtitle = mediaSubtitle,
        thumbnailContent = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(ListThumbnailSize),
            ) {
                if (isSelected) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .zIndex(1000f)
                                .clip(RoundedCornerShape(ThumbnailCornerRadius))
                                .background(Color.Black.copy(alpha = 0.5f)),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.done),
                            modifier = Modifier.align(Alignment.Center),
                            contentDescription = null,
                        )
                    }
                }

                AsyncImage(
                    model = mediaMetadata.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                )

                PlayingIndicatorBox(
                    isActive = isActive,
                    playWhenReady = isPlaying,
                    color = Color.White,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                color = Color.Black.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(ThumbnailCornerRadius),
                            ),
                )
            }
        },
        trailingContent = trailingContent,
        modifier = modifier,
        isActive = isActive,
    )
}

@Composable
fun YouTubeListItem(
    item: YTItem,
    modifier: Modifier = Modifier,
    albumIndex: Int? = null,
    isSelected: Boolean = false,
    badges: @Composable RowScope.() -> Unit = {
        val database = LocalDatabase.current
        val song by database.song(item.id).collectAsState(initial = null)
        val album by database.album(item.id).collectAsState(initial = null)

        if (item is SongItem &&
            song?.song?.liked == true ||
            item is AlbumItem &&
            album?.album?.bookmarkedAt != null
        ) {
            Icon(
                painter = painterResource(R.drawable.favorite),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }
        if (item.explicit) {
            Icon(
                painter = painterResource(R.drawable.explicit),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }
        if (item is SongItem && song?.song?.inLibrary != null) {
            Icon(
                painter = painterResource(R.drawable.library_add_check),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }
        if (item is SongItem) {
            val downloads by LocalDownloadUtil.current.downloads.collectAsState()
            when (downloads[item.id]?.state) {
                STATE_COMPLETED ->
                    Icon(
                        painter = painterResource(R.drawable.offline),
                        contentDescription = null,
                        modifier =
                            Modifier
                                .size(18.dp)
                                .padding(end = 2.dp),
                    )

                STATE_QUEUED, STATE_DOWNLOADING ->
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier =
                            Modifier
                                .size(16.dp)
                                .padding(end = 2.dp),
                    )

                else -> {}
            }
        }
    },
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit = {},
) = ListItem(
    title = item.title,
    subtitle =
        when (item) {
            is SongItem -> joinByBullet(
                item.artists.joinToString { it.name },
                makeTimeString(item.duration?.times(1000L))
            )

            is AlbumItem -> joinByBullet(
                item.artists?.joinToString { it.name },
                item.year?.toString()
            )

            is ArtistItem -> null
            is PlaylistItem -> joinByBullet(item.author?.name, item.songCountText)
        },
    badges = badges,
    thumbnailContent = {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(ListThumbnailSize),
        ) {
            val thumbnailShape =
                if (item is ArtistItem) CircleShape else RoundedCornerShape(ThumbnailCornerRadius)
            if (albumIndex != null) {
                AnimatedVisibility(
                    visible = !isActive,
                    enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
                    exit = shrinkOut(shrinkTowards = Alignment.Center) + fadeOut(),
                ) {
                    if (isSelected) {
                        Icon(
                            painter = painterResource(R.drawable.done),
                            modifier = Modifier.align(Alignment.Center),
                            contentDescription = null,
                        )
                    } else {
                        Text(
                            text = albumIndex.toString(),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            } else {
                if (isSelected) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .zIndex(1000f)
                                .clip(RoundedCornerShape(ThumbnailCornerRadius))
                                .background(Color.Black.copy(alpha = 0.5f)),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.done),
                            modifier = Modifier.align(Alignment.Center),
                            contentDescription = null,
                        )
                    }
                }
                AsyncImage(
                    model = item.thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(thumbnailShape),
                )
            }

            PlayingIndicatorBox(
                isActive = isActive,
                playWhenReady = isPlaying,
                color = if (albumIndex != null) MaterialTheme.colorScheme.onBackground else Color.White,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            color =
                                if (albumIndex != null) {
                                    Color.Transparent
                                } else {
                                    Color.Black.copy(
                                        alpha = 0.4f,
                                    )
                                },
                            shape = thumbnailShape,
                        ),
            )
        }
    },
    trailingContent = trailingContent,
    modifier = modifier,
    isActive = isActive,
)

@SuppressLint("SuspiciousIndentation")
@Composable
fun YouTubeGridItem(
    item: YTItem,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope? = null,
    badges: @Composable RowScope.() -> Unit = {
        val database = LocalDatabase.current
        val song by database.song(item.id).collectAsState(initial = null)
        val album by database.album(item.id).collectAsState(initial = null)

        if (item is SongItem &&
            song?.song?.liked == true ||
            item is AlbumItem &&
            album?.album?.bookmarkedAt != null
        ) {
            Icon(
                painter = painterResource(R.drawable.favorite),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }
        if (item is SongItem && song?.song?.inLibrary != null) {
            Icon(
                painter = painterResource(R.drawable.library_add_check),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }
        if (item.explicit) {
            Icon(
                painter = painterResource(R.drawable.explicit),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }
        if (item is SongItem) {
            val downloads by LocalDownloadUtil.current.downloads.collectAsState()
            when (downloads[item.id]?.state) {
                STATE_COMPLETED ->
                    Icon(
                        painter = painterResource(R.drawable.offline),
                        contentDescription = null,
                        modifier =
                            Modifier
                                .size(18.dp)
                                .padding(end = 2.dp),
                    )

                STATE_DOWNLOADING ->
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier =
                            Modifier
                                .size(16.dp)
                                .padding(end = 2.dp),
                    )

                else -> {}
            }
        }
    },
    thumbnailRatio: Float = if (item is SongItem) 16f / 9 else 1f,
    thumbnailShape: Shape = if (item is ArtistItem) CircleShape else RoundedCornerShape(ThumbnailCornerRadius),
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
    imageSize: Int = 512, // Default size for grid items
) {
    val thumbnailShape = thumbnailShape
    val thumbnailRatio = thumbnailRatio

    Column(
        modifier =
            if (fillMaxWidth) {
                modifier
                    .padding(12.dp)
                    .fillMaxWidth()
            } else {
                modifier
                    .padding(12.dp)
                    .width(GridThumbnailHeight * thumbnailRatio)
            },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .fillMaxSize()
                    .aspectRatio(thumbnailRatio)
                    .clip(thumbnailShape),
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.thumbnail)
                    .crossfade(true)
                    .size(imageSize)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(thumbnailShape),
            )

            androidx.compose.animation.AnimatedVisibility(
                visible = item is AlbumItem && !isActive,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
            ) {
                val database = LocalDatabase.current
                val playerConnection = LocalPlayerConnection.current ?: return@AnimatedVisibility

                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable {
                                var playlistId = ""
                                coroutineScope?.launch(Dispatchers.IO) {
                                    var albumWithSongs = database.albumWithSongs(item.id).first()
                                    if (albumWithSongs?.songs.isNullOrEmpty()) {
                                        YouTube
                                            .album(item.id)
                                            .onSuccess { albumPage ->
                                                playlistId = albumPage.album.playlistId
                                                database.transaction {
                                                    insert(albumPage)
                                                }
                                                albumWithSongs =
                                                    database.albumWithSongs(item.id).first()
                                            }.onFailure {
                                                reportException(it)
                                            }
                                    }
                                    albumWithSongs?.let {
                                        withContext(Dispatchers.Main) {
                                            playerConnection.service.getAutomix(playlistId)
                                            playerConnection.playQueue(
                                                LocalAlbumRadio(it),
                                            )
                                        }
                                    }
                                }
                            },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = isActive,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(500)),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                color = Color.Black.copy(alpha = if (isPlaying) 0.4f else 0f),
                                shape = thumbnailShape,
                            ),
                ) {
                    if (isPlaying) {
                        PlayingIndicator(
                            color = Color.White,
                            modifier = Modifier.height(24.dp),
                        )
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = item is SongItem && !(isActive && isPlaying),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .padding(8.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f)),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        DisableSelection {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (item is ArtistItem) TextAlign.Center else TextAlign.Start,
                modifier =
                    Modifier
                        .basicMarquee(iterations = 3)
                        .fillMaxWidth(),
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            badges()

            val subtitle =
                when (item) {
                    is SongItem -> joinByBullet(
                        item.artists.joinToString { it.name },
                        makeTimeString(item.duration?.times(1000L))
                    )

                    is AlbumItem -> joinByBullet(
                        item.artists?.joinToString { it.name },
                        item.year?.toString()
                    )

                    is ArtistItem -> null
                    is PlaylistItem -> joinByBullet(item.author?.name, item.songCountText)
                }

            if (subtitle != null) {
                DisableSelection {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun YouTubeSmallGridItem(
    item: YTItem,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope? = null,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
) = SmallGridItem(
    title = item.title,
    thumbnailContent = {
        AsyncImage(
            model = item.thumbnail,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(ThumbnailCornerRadius)),
        )
        if (item is SongItem) {
            AnimatedVisibility(
                visible = isActive,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(500)),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                color = Color.Black.copy(alpha = if (isPlaying) 0.4f else 0f),
                                shape = RoundedCornerShape(ThumbnailCornerRadius),
                            ),
                ) {
                    if (isPlaying) {
                        PlayingIndicator(
                            color = Color.White,
                            modifier = Modifier.height(24.dp),
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !(isActive && isPlaying),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .padding(8.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f)),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
            }
        }
    },
    thumbnailShape =
        when (item) {
            is ArtistItem -> CircleShape
            else -> RoundedCornerShape(ThumbnailCornerRadius)
        },
    modifier = modifier,
    isArtist =
        when (item) {
            is ArtistItem -> true
            else -> false
        },
)

@Composable
fun LocalSongsGrid(
    title: String,
    subtitle: String,
    badges:
    @Composable()
    (RowScope.() -> Unit) = {},
    thumbnailUrl: String?,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
    modifier: Modifier,
) = GridItem(
    title = title,
    subtitle = subtitle,
    badges = badges,
    thumbnailContent = {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(ThumbnailCornerRadius)),
        ) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth(),
            )

            AnimatedVisibility(
                visible = isActive,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(500)),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                color = Color.Black.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(ThumbnailCornerRadius),
                            ),
                ) {
                    if (isPlaying) {
                        PlayingIndicator(
                            color = Color.White,
                            modifier = Modifier.height(24.dp),
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !(isActive && isPlaying),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .padding(8.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f)),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
            }
        }
    },
    thumbnailShape = RoundedCornerShape(ThumbnailCornerRadius),
    fillMaxWidth = fillMaxWidth,
    modifier = modifier,
)

@Composable
fun LocalArtistsGrid(
    title: String,
    subtitle: String,
    badges:
    @Composable()
    (RowScope.() -> Unit) = {},
    thumbnailUrl: String?,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
    modifier: Modifier,
) = GridItem(
    title = title,
    subtitle = subtitle,
    badges = badges,
    thumbnailContent = {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape),
        ) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            AnimatedVisibility(
                visible = isActive,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(500)),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                color = Color.Black.copy(alpha = 0.4f),
                                shape = CircleShape,
                            ),
                ) {
                    if (isPlaying) {
                        PlayingIndicator(
                            color = Color.White,
                            modifier = Modifier.height(24.dp),
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                }
            }
        }
    },
    thumbnailShape = CircleShape,
    fillMaxWidth = fillMaxWidth,
    modifier = modifier,
)

@Composable
fun LocalAlbumsGrid(
    title: String,
    subtitle: String,
    badges:
    @Composable()
    (RowScope.() -> Unit) = {},
    thumbnailUrl: String?,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
    modifier: Modifier,
) = GridItem(
    title = title,
    subtitle = subtitle,
    badges = badges,
    thumbnailContent = {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedVisibility(
            visible = isActive,
            enter = fadeIn(tween(500)),
            exit = fadeOut(tween(500)),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            color = Color.Black.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(ThumbnailCornerRadius),
                        ),
            ) {
                if (isPlaying) {
                    PlayingIndicator(
                        color = Color.White,
                        modifier = Modifier.height(24.dp),
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
            }
        }
    },
    thumbnailShape = RoundedCornerShape(ThumbnailCornerRadius),
    fillMaxWidth = fillMaxWidth,
    modifier = modifier,
)

@Composable
fun LibraryListItem(
    title: String,
    subtitle: String,
    imageUrl: String? = null,
    iconRes: Int? = null,
    gradientColors: List<Color>? = null,
    trailingContent: @Composable (RowScope.() -> Unit)? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "LibraryListItemScale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { scope.launch { onClick() } },
                onLongClick = { onLongClick?.let { scope.launch { it() } } }
            )
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // IMAGEN O ICONO
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (gradientColors != null) {
                        Brush.linearGradient(gradientColors)
                    } else {
                        Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant))
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = if (gradientColors != null) Color.White else MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        // TEXTOS
        Column(modifier = Modifier.weight(1f)) {
            DisableSelection {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            DisableSelection {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (trailingContent != null) {
            trailingContent()
        }
    }
}

@Composable
fun ArtistCircleItem(
    artistName: String,
    thumbnailUrl: String?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "ArtistCircleItemScale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { scope.launch { onClick() } },
                onLongClick = { onLongClick?.let { scope.launch { it() } } }
            )
            .padding(vertical = 10.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // La foto oficial del artista
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(thumbnailUrl ?: R.drawable.drawable_ic_kuro)
                .crossfade(true)
                .build(),
            contentDescription = artistName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
            Text(
                text = artistName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.filter_artists),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
@Composable
fun LibraryArtistItem(
    artistName: String,
    thumbnailUrl: String?, // Este viene de la DB enriquecida
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isScrolling: Boolean = false,
) {
    val scope = rememberCoroutineScope()
    
    // Throttling: solo cargamos la imagen si no se está haciendo scroll, 
    // o si ya se cargó previamente (para no borrarla al volver a hacer scroll).
    var shouldLoad by remember { mutableStateOf(!isScrolling) }
    LaunchedEffect(isScrolling) {
        if (!isScrolling) shouldLoad = true
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { scope.launch { onClick() } },
                onLongClick = { onLongClick?.let { scope.launch { it() } } }
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(if (shouldLoad) thumbnailUrl else null)
                .crossfade(true)
                .size(256) // Reducir consumo de VRAM
                // Usamos Kuro como placeholder mientras carga o si falla
                .placeholder(R.drawable.drawable_ic_kuro)
                .error(R.drawable.drawable_ic_kuro)
                .build(),
            contentDescription = artistName,
            modifier = Modifier
                .size(65.dp)
                .clip(CircleShape), // Foto redonda estilo Spotify
            contentScale = ContentScale.Crop
        )

        Text(
            text = artistName,
            modifier = Modifier.padding(start = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
