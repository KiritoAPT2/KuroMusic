package com.kuromusic.playback

enum class Genre {
    LOFI,
    BASS_MUSIC,
    POP,
    ROCK,
    PODCAST,
    UNKNOWN,
}

object GenreDetector {

    fun detect(title: String): Genre {
        val t = title.lowercase()

        val scores = mapOf(
            Genre.LOFI to scoreLofi(t),
            Genre.BASS_MUSIC to scoreBass(t),
            Genre.ROCK to scoreRock(t),
            Genre.PODCAST to scorePodcast(t),
        )

        val best = scores.maxBy { it.value }
        return if (best.value < 2) Genre.UNKNOWN else best.key
    }

    private fun scoreLofi(t: String): Int {
        var s = 0
        if (t.contains("lofi") || t.contains("lo-fi")) s += 5
        if (t.contains("chill") || t.contains("chillout")) s += 3
        if (t.contains("study") || t.contains("sleep") || t.contains("relax")) s += 2
        if (t.contains("jazz") || t.contains("ambient")) s += 2
        if (t.contains("beats") || t.contains("instrumental")) s += 1
        return s
    }

    private fun scoreBass(t: String): Int {
        var s = 0
        if (t.contains("bass")) s += 5
        if (t.contains("trap")) s += 4
        if (t.contains("dubstep") || t.contains("edm")) s += 3
        if (t.contains("electronic") || t.contains("dnb")) s += 2
        if (t.contains("remix") || t.contains("club")) s += 1
        return s
    }

    private fun scoreRock(t: String): Int {
        var s = 0
        if (t.contains("rock")) s += 5
        if (t.contains("metal") || t.contains("punk")) s += 4
        if (t.contains("alternative") || t.contains("indie")) s += 2
        if (t.contains("hard") || t.contains("heavy")) s += 1
        return s
    }

    private fun scorePodcast(t: String): Int {
        var s = 0
        if (t.contains("podcast") || t.contains("episode")) s += 5
        if (t.contains("interview") || t.contains("talk")) s += 3
        if (t.contains("news") || t.contains("story")) s += 2
        return s
    }
}
