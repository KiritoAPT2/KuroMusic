package com.grp2.kuromusic.data

data class AnimaxSong(
    val title: String,
    val ytId: String,
    val thumbnailUrl: String
)

val ANIMAX_RECOMMENDATIONS = listOf(
    AnimaxSong("Dreams (Cover Español) - Bolbbalgan4", "FImxqxDv5KY", "https://i.ytimg.com/vi/FImxqxDv5KY/maxresdefault.jpg"),
    AnimaxSong("Como un tonto (Cover español) - Jeong Seon-yeon", "gHFhOY9XTYE", "https://i.ytimg.com/vi/gHFhOY9XTYE/maxresdefault.jpg"),
    AnimaxSong("Tierra bendita (Remaster) - Animax Music's", "OPIHlTY21RQ", "https://i.ytimg.com/vi/OPIHlTY21RQ/maxresdefault.jpg"),
    AnimaxSong("Himno del Guerrero - Animax Music's", "1ZvGcaG_xkk", "https://i.ytimg.com/vi/1ZvGcaG_xkk/maxresdefault.jpg"),
    AnimaxSong("Falta de Atención - Animax Music's", "zuXaIyxihfc", "https://i.ytimg.com/vi/zuXaIyxihfc/maxresdefault.jpg")
)
