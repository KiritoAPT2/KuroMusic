package com.kuromusic.ui.utils

fun String.resize(
    width: Int? = null,
    height: Int? = null,
): String {
    if (width == null && height == null) return this
    "https://lh3\\.googleusercontent\\.com/.*=w(\\d+)-h(\\d+).*".toRegex()
        .matchEntire(this)?.groupValues?.let { group ->
            val (W, H) = group.drop(1).map { it.toInt() }
            var w = width
            var h = height
            if (w != null && h == null) h = (w / W) * H
            if (w == null && h != null) w = (h / H) * W
            return "${split("=w")[0]}=w$w-h$h-p-l90-rj"
        }
    if (this matches "https://yt3\\.ggpht\\.com/.*=s(\\d+)".toRegex()) {
        return "$this-s${width ?: height}"
    }
    
    // Forzar alta resolución para carátulas de YouTube (i.ytimg.com / img.youtube.com)
    if (this.contains("i.ytimg.com/vi/") || this.contains("img.youtube.com/vi/")) {
        val videoId = this.split("/vi/").getOrNull(1)?.split("/")?.getOrNull(0)
        if (videoId != null) {
            val quality = if ((width ?: 0) > 400 || (height ?: 0) > 400) "sddefault.jpg" else "hqdefault.jpg"
            return "https://i.ytimg.com/vi/$videoId/$quality"
        }
    }
    
    return this
}
