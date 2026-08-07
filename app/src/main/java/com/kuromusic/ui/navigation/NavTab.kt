package com.kuromusic.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.kuromusic.R
import com.kuromusic.ui.screens.Screens

enum class NavTab(
    @StringRes val titleRes: Int,
    @DrawableRes val inactiveIcon: Int,
    @DrawableRes val activeIcon: Int,
    val route: String,
) {
    HOME(
        titleRes = R.string.home,
        inactiveIcon = R.drawable.home_outlined,
        activeIcon = R.drawable.home_filled,
        route = Screens.Home.route,
    ),
    EXPLORE(
        titleRes = R.string.explore,
        inactiveIcon = R.drawable.explore_outlined,
        activeIcon = R.drawable.explore_filled,
        route = Screens.Explore.route,
    ),
    LIBRARY(
        titleRes = R.string.filter_library,
        inactiveIcon = R.drawable.library_music_outlined,
        activeIcon = R.drawable.library_music_filled,
        route = Screens.Library.route,
    ),
    OFFLINE(
        titleRes = R.string.offline,
        inactiveIcon = R.drawable.download,
        activeIcon = R.drawable.download,
        route = Screens.Offline.route,
    ),
    ;

    companion object {
        val defaults = listOf(HOME, EXPLORE, LIBRARY, OFFLINE)
    }
}
