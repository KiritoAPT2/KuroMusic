package com.kuromusic.utils

import java.math.BigInteger
import java.security.MessageDigest

fun makeTimeString(duration: Long?): String {
    if (duration == null || duration < 0) return ""
    
    var sec = duration / 1000
    val day = sec / 86400
    sec %= 86400
    val hour = sec / 3600
    sec %= 3600
    val minute = sec / 60
    sec %= 60
    return when {
        day > 0 -> "%d:%02d:%02d:%02d".format(day, hour, minute, sec)
        hour > 0 -> "%d:%02d:%02d".format(hour, minute, sec)
        else -> "%d:%02d".format(minute, sec)
    }
}

fun formatCount(count: Long?): String {
    if (count == null || count == 0L) return "0"
    
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
            .replace(".0M", "M")  // 1.0M -> 1M
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
            .replace(".0K", "K")  // 1.0K -> 1K
        else -> count.toString()
    }
}

fun md5(str: String): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(str.toByteArray())).toString(16).padStart(32, '0')
}

fun joinByBullet(vararg str: String?) =
    str
        .filterNot {
            it.isNullOrEmpty()
        }.joinToString(separator = " • ")
