package com.kuromusic

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * The home-screen icon choices. Each is an `<activity-alias>` in the manifest
 * carrying the launcher intent filter; exactly one is enabled at a time.
 *
 * All variants use the KuroMusic logo foreground over different coloured
 * backgrounds. The first two keep yesterday's alias names so a choice made
 * before the update stays enabled.
 */
enum class AppIcon(val alias: String, val iconRes: Int, val labelRes: Int, val previewRes: Int? = null) {
    Default("LauncherDefault", R.mipmap.ic_launcher, R.string.app_icon_default),
    Monochrome("LauncherMonochrome", R.mipmap.ic_monochrome, R.string.app_icon_monochrome),
    Purple("LauncherPurple", R.mipmap.ic_kuro_purple, R.string.app_icon_purple, R.drawable.ic_kuro_purple_fg),
    Dark("LauncherDark", R.mipmap.ic_kuro_dark, R.string.app_icon_dark, R.drawable.ic_kuro_dark_fg),
    Cyan("LauncherCyan", R.mipmap.ic_kuro_cyan, R.string.app_icon_cyan, R.drawable.ic_kuro_cyan_fg),
    Pink("LauncherPink", R.mipmap.ic_kuro_pink, R.string.app_icon_pink, R.drawable.ic_kuro_pink_fg),
    White("LauncherWhite", R.mipmap.ic_kuro_white, R.string.app_icon_white, R.drawable.ic_kuro_white_fg),
    Emerald("LauncherEmerald", R.mipmap.ic_kuro_emerald, R.string.app_icon_emerald, R.drawable.ic_kuro_emerald_fg);

    private fun component(context: Context) =
        ComponentName(context.packageName, "com.kuromusic.$alias")

    companion object {
        fun current(context: Context): AppIcon {
            val pm = context.packageManager
            return entries.firstOrNull {
                pm.getComponentEnabledSetting(it.component(context)) ==
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } ?: Default
        }

        /** Enables [icon] before disabling the rest so there is never a moment with no launcher entry. */
        fun apply(context: Context, icon: AppIcon) {
            val pm = context.packageManager
            pm.setComponentEnabledSetting(
                icon.component(context),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
            entries.filter { it != icon }.forEach {
                pm.setComponentEnabledSetting(
                    it.component(context),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }
        }
    }
}
