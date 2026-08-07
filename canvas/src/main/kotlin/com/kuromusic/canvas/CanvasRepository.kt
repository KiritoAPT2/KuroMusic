package com.kuromusic.canvas

public class CanvasRepository {

    public suspend fun getCanvas(
        song: String,
        artist: String,
        album: String? = null,
    ): CanvasArtwork? {
        if (artist.isBlank()) return null

        // 1. Try Tidal
        val tidalResult = TidalCanvasProvider.getBySongArtist(song, artist, album)
        if (tidalResult != null) return tidalResult

        if (!album.isNullOrBlank()) {
            val tidalAlbumResult = TidalCanvasProvider.getByAlbumArtist(album, artist)
            if (tidalAlbumResult != null) return tidalAlbumResult
        }

        // 2. Try artist video archive
        val artistVideoResult = ArtistVideoCanvasProvider.getBySongArtist(song, artist, album)
        if (artistVideoResult != null) return artistVideoResult

        // 3. Try Echo community canvas
        val echoResult = EchoMusicCanvasProvider.getBySongArtist(song, artist)
        if (echoResult != null) return echoResult

        return null
    }

    public suspend fun getArtistBackground(
        artist: String,
    ): String? {
        if (artist.isBlank()) return null
        return AppleMusicArtistBackgroundProvider.getByArtistName(artist)
    }
}
