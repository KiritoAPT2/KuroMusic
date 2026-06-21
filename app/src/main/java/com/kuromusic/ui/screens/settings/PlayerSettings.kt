package com.kuromusic.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kuromusic.LocalPlayerAwareWindowInsets
import com.kuromusic.R
import com.kuromusic.constants.AudioNormalizationKey
import com.kuromusic.constants.AudioQuality
import com.kuromusic.constants.AudioQualityKey
import com.kuromusic.constants.AutoLoadMoreKey
import com.kuromusic.constants.AutoSkipNextOnErrorKey
import com.kuromusic.constants.BeatBuddyType
import com.kuromusic.constants.BeatBuddyTypeKey
import com.kuromusic.constants.PersistentQueueKey
import com.kuromusic.constants.ProfileModeKey
import com.kuromusic.constants.SimilarContent
import com.kuromusic.constants.SkipSilenceKey
import com.kuromusic.constants.SoundProfileKey
import com.kuromusic.constants.StopMusicOnTaskClearKey
import com.kuromusic.playback.ProfileMode
import com.kuromusic.playback.SoundProfile
import com.kuromusic.ui.component.EnumListPreference
import com.kuromusic.ui.component.IconButton
import com.kuromusic.ui.component.PreferenceGroupTitle
import com.kuromusic.ui.component.SettingsGeneralCategory
import com.kuromusic.ui.component.SettingsPage
import com.kuromusic.ui.component.SwitchPreference
import com.kuromusic.ui.utils.backToMain
import com.kuromusic.utils.rememberEnumPreference
import com.kuromusic.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (audioQuality, onAudioQualityChange) = rememberEnumPreference(
        AudioQualityKey,
        defaultValue = AudioQuality.AUTO
    )
    val (persistentQueue, onPersistentQueueChange) = rememberPreference(
        PersistentQueueKey,
        defaultValue = true
    )
    val (skipSilence, onSkipSilenceChange) = rememberPreference(
        SkipSilenceKey,
        defaultValue = false
    )
    val (audioNormalization, onAudioNormalizationChange) = rememberPreference(
        AudioNormalizationKey,
        defaultValue = true
    )
    val (autoLoadMore, onAutoLoadMoreChange) = rememberPreference(
        AutoLoadMoreKey,
        defaultValue = true
    )
    val (similarContentEnabled, similarContentEnabledChange) = rememberPreference(
        key = SimilarContent,
        defaultValue = true
    )
    val (autoSkipNextOnError, onAutoSkipNextOnErrorChange) = rememberPreference(
        AutoSkipNextOnErrorKey,
        defaultValue = false
    )
    val (stopMusicOnTaskClear, onStopMusicOnTaskClearChange) = rememberPreference(
        StopMusicOnTaskClearKey,
        defaultValue = false
    )
    val (beatBuddyType, onBeatBuddyTypeChange) = rememberEnumPreference(
        BeatBuddyTypeKey,
        defaultValue = BeatBuddyType.NONE
    )
    val (soundProfile, onSoundProfileChange) = rememberEnumPreference(
        SoundProfileKey,
        defaultValue = SoundProfile.CLEAN
    )
    val (profileMode, onProfileModeChange) = rememberEnumPreference(
        ProfileModeKey,
        defaultValue = ProfileMode.MANUAL
    )

    SettingsPage(
        title = stringResource(R.string.player_and_audio),
        navController = navController,
        scrollBehavior = scrollBehavior
    ) {
        SettingsGeneralCategory(
            title = stringResource(R.string.player),
            items = listOf(
                {EnumListPreference(
                    title = { Text(stringResource(R.string.audio_quality)) },
                    icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
                    selectedValue = audioQuality,
                    onValueSelected = onAudioQualityChange,
                    valueText = {
                        when (it) {
                            AudioQuality.AUTO -> stringResource(R.string.audio_quality_auto)
                            AudioQuality.HIGH -> stringResource(R.string.audio_quality_high)
                            AudioQuality.LOW -> stringResource(R.string.audio_quality_low)
                        }
                    }
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.skip_silence)) },
                    icon = { Icon(painterResource(R.drawable.fast_forward), null) },
                    checked = skipSilence,
                    onCheckedChange = onSkipSilenceChange
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.audio_normalization)) },
                    icon = { Icon(painterResource(R.drawable.volume_up), null) },
                    checked = audioNormalization,
                    onCheckedChange = onAudioNormalizationChange
                )},

                {EnumListPreference(
                    title = { Text(stringResource(R.string.profile_mode)) },
                    icon = { Icon(painterResource(R.drawable.tune), null) },
                    selectedValue = profileMode,
                    onValueSelected = onProfileModeChange,
                    valueText = {
                        when (it) {
                            ProfileMode.AUTO -> stringResource(R.string.profile_mode_auto)
                            ProfileMode.MANUAL -> stringResource(R.string.profile_mode_manual)
                        }
                    }
                )},

                {EnumListPreference(
                    title = { Text(stringResource(R.string.sound_profile)) },
                    icon = { Icon(painterResource(R.drawable.tune), null) },
                    selectedValue = soundProfile,
                    onValueSelected = onSoundProfileChange,
                    valueText = {
                        when (it) {
                            SoundProfile.CLEAN -> stringResource(R.string.sound_profile_clean)
                            SoundProfile.WARM -> stringResource(R.string.sound_profile_warm)
                            SoundProfile.BASS -> stringResource(R.string.sound_profile_bass)
                            SoundProfile.LOFI -> stringResource(R.string.sound_profile_lofi)
                            SoundProfile.STUDIO -> stringResource(R.string.sound_profile_studio)
                        }
                    }
                )},

                {
                    Text(
                        text = stringResource(R.string.sound_profile_note),
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                },

                {EnumListPreference(
                    title = { Text(stringResource(R.string.beat_buddy)) },
                    icon = { Icon(painterResource(R.drawable.mood), null) },
                    selectedValue = beatBuddyType,
                    onValueSelected = onBeatBuddyTypeChange,
                    valueText = {
                        when (it) {
                            BeatBuddyType.NONE -> stringResource(R.string.beat_buddy_none)
                            BeatBuddyType.CAT -> stringResource(R.string.beat_buddy_cat)
                            BeatBuddyType.BEAR -> stringResource(R.string.beat_buddy_bear)
                        }
                    }
                )},
            )
        )

        SettingsGeneralCategory(
            title = stringResource(R.string.queue),
            items = listOf(
                {SwitchPreference(
                    title = { Text(stringResource(R.string.persistent_queue)) },
                    description = stringResource(R.string.persistent_queue_desc),
                    icon = { Icon(painterResource(R.drawable.queue_music), null) },
                    checked = persistentQueue,
                    onCheckedChange = onPersistentQueueChange
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.auto_load_more)) },
                    description = stringResource(R.string.auto_load_more_desc),
                    icon = { Icon(painterResource(R.drawable.playlist_add), null) },
                    checked = autoLoadMore,
                    onCheckedChange = onAutoLoadMoreChange
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.enable_similar_content)) },
                    description = stringResource(R.string.similar_content_desc),
                    icon = { Icon(painterResource(R.drawable.similar), null) },
                    checked = similarContentEnabled,
                    onCheckedChange = similarContentEnabledChange,
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.auto_skip_next_on_error)) },
                    description = stringResource(R.string.auto_skip_next_on_error_desc),
                    icon = { Icon(painterResource(R.drawable.skip_next), null) },
                    checked = autoSkipNextOnError,
                    onCheckedChange = onAutoSkipNextOnErrorChange
                )},
            )
        )

        SettingsGeneralCategory(
            title = stringResource(R.string.misc),
            items = listOf(
                {SwitchPreference(
                    title = { Text(stringResource(R.string.stop_music_on_task_clear)) },
                    icon = { Icon(painterResource(R.drawable.clear_all), null) },
                    checked = stopMusicOnTaskClear,
                    onCheckedChange = onStopMusicOnTaskClearChange
                )},
            )
        )
    }
}