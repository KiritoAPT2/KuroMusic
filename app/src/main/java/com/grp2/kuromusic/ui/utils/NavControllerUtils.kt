package com.grp2.kuromusic.ui.utils

import androidx.compose.ui.util.fastAny
import androidx.navigation.NavController
import com.grp2.kuromusic.ui.screens.Screens

val NavController.canNavigateUp: Boolean
    get() = currentBackStackEntry?.destination?.parent?.route != null

fun NavController.backToMain() {
    while (canNavigateUp && !Screens.MainScreens.fastAny { it.route == currentBackStackEntry?.destination?.route }) {
        navigateUp()
    }
}
