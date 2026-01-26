package com.kuromusic.ui.screens

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.kuromusic.LocalPlayerAwareWindowInsets
import com.kuromusic.R
import com.kuromusic.ui.component.shimmer.ListItemPlaceHolder
import com.kuromusic.ui.component.shimmer.ShimmerHost
import com.kuromusic.ui.utils.backToMain
import com.kuromusic.viewmodels.MoodAndGenresViewModel
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: MoodAndGenresViewModel = hiltViewModel(),
) {
    val localConfiguration = LocalConfiguration.current
    val itemsPerRow = if (localConfiguration.orientation == ORIENTATION_LANDSCAPE) 3 else 2

    val moodAndGenresList by viewModel.moodAndGenres.collectAsState()

    LazyColumn(
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Buscador Estilo Spotify
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(
                            width = 1.dp,
                            color = Color.LightGray.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                             navController.navigate("search/%20")
                        }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = Color.Black, // Mantenemos Negro en el buscador blanco fijo por estética Spotify
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.search_yt_music),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Black.copy(alpha = 0.7f), // Mantenemos Negro en el buscador blanco fijo
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (moodAndGenresList == null) {
            item {
                ShimmerHost {
                    repeat(8) {
                        ListItemPlaceHolder()
                    }
                }
            }
        }

        moodAndGenresList?.forEach { moodAndGenres ->
            item {
                Text(
                    text = moodAndGenres.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    moodAndGenres.items.chunked(itemsPerRow).forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            row.forEach {
                                MoodAndGenresButton(
                                    title = it.title,
                                    stripeColor = it.stripeColor,
                                    thumbnailUrl = it.thumbnailUrl,
                                    onClick = {
                                        navController.navigate("youtube_browse/${it.endpoint.browseId}?params=${it.endpoint.params}")
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }

                            repeat(itemsPerRow - row.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MoodAndGenresButton(
    title: String,
    stripeColor: Long,
    thumbnailUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseColor = remember(stripeColor) { Color(stripeColor) }
    
    Box(
        modifier = modifier
            .height(104.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(baseColor)
            .clickable(onClick = onClick)
            .clipToBounds() // Asegura que la imagen rotada no se salga
    ) {
        // Texto con mejor jerarquía
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 20.sp
            ),
            color = if (baseColor.luminance() > 0.5f) Color.Black else Color.White,
            modifier = Modifier
                .padding(12.dp)
                .align(Alignment.TopStart)
                .fillMaxWidth(0.6f) // Evita que el texto pise la imagen
        )
        
        // Imagen dinámica estilo Spotify
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 24.dp, y = 12.dp)
                .graphicsLayer(rotationZ = 25f)
                .shadow(8.dp, RoundedCornerShape(4.dp))
        ) {
            AsyncImage(
                model = thumbnailUrl ?: R.drawable.drawable_ic_kuro, // Fallback a ic_kuro si no hay thumb
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.1f)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

val MoodAndGenresButtonHeight = 48.dp
