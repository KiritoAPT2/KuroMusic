package com.kuromusic.ui.screens.equalizer

import android.content.Intent
import android.media.audiofx.AudioEffect
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavController
import com.kuromusic.LocalPlayerConnection
import com.kuromusic.R
import com.kuromusic.eq.EQViewModel
import com.kuromusic.eq.data.SavedEQProfile
import timber.log.Timber

@Composable
fun EqScreen(
    navController: NavController? = null,
    viewModel: EQViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current

    var showError by remember { mutableStateOf<String?>(null) }

    val activityResultLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val contentResolver = context.contentResolver
                var fileName = "custom_eq.txt"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (displayNameIndex >= 0) {
                            val name = cursor.getString(displayNameIndex)
                            if (!name.isNullOrBlank()) fileName = name
                        }
                    }
                }
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    viewModel.importCustomProfile(
                        fileName = fileName,
                        inputStream = inputStream,
                        onSuccess = { Timber.d("Custom EQ profile imported: $fileName") },
                        onError = { showError = "Import error: ${it.message}" }
                    )
                } else {
                    showError = "Error reading file"
                }
            } catch (e: Exception) {
                showError = "Error: ${e.message}"
            }
        }
    }

    EqScreenContent(
        profiles = state.profiles,
        activeProfileId = state.activeProfileId,
        onProfileSelected = { viewModel.selectProfile(it) },
        onImportCustomEQ = { filePickerLauncher.launch("text/plain") },
        onOpenSystemEqualizer = {
            playerConnection?.let { conn ->
                val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                    putExtra(AudioEffect.EXTRA_AUDIO_SESSION, conn.player.audioSessionId)
                    putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                    putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    activityResultLauncher.launch(intent)
                }
            }
        },
        onNavigateToAxion = { navController?.navigate("settings/equalizer") },
        onDeleteProfile = { viewModel.deleteProfile(it) }
    )

    if (showError != null) {
        AlertDialog(
            onDismissRequest = { showError = null },
            title = { Text("Import Error") },
            text = { Text(showError ?: "") },
            confirmButton = { TextButton(onClick = { showError = null }) { Text("OK") } }
        )
    }

    if (state.error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text("Failed to apply EQ: ${state.error}") },
            confirmButton = { TextButton(onClick = { viewModel.clearError() }) { Text("OK") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EqScreenContent(
    profiles: List<SavedEQProfile>,
    activeProfileId: String?,
    onProfileSelected: (String?) -> Unit,
    onImportCustomEQ: () -> Unit,
    onOpenSystemEqualizer: () -> Unit,
    onNavigateToAxion: () -> Unit,
    onDeleteProfile: (String) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .heightIn(max = 600.dp)
            .padding(vertical = 24.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Equalizer", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "${profiles.size} profile(s)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = onNavigateToAxion) {
                        Icon(
                            painter = painterResource(R.drawable.tune),
                            contentDescription = "Tune"
                        )
                    }
                    IconButton(onClick = onImportCustomEQ) {
                        Icon(Icons.Default.Add, contentDescription = "Import")
                    }
                    IconButton(onClick = onOpenSystemEqualizer) {
                        Icon(
                            painter = painterResource(R.drawable.equalizer),
                            contentDescription = "System EQ"
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    NoEqualizationItem(
                        isSelected = activeProfileId == null,
                        onSelected = { onProfileSelected(null) }
                    )
                }

                val customProfiles = profiles.filter { it.isCustom }

                if (customProfiles.isNotEmpty()) {
                    items(customProfiles) { profile ->
                        EQProfileItem(
                            profile = profile,
                            isSelected = activeProfileId == profile.id,
                            onSelected = { onProfileSelected(profile.id) },
                            onDelete = { onDeleteProfile(profile.id) }
                        )
                    }
                }

                if (customProfiles.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    painter = painterResource(R.drawable.equalizer),
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "No profiles",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = onImportCustomEQ) {
                                    Text("Import Profile")
                                }
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(onClick = onOpenSystemEqualizer) {
                                    Text("System Equalizer")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoEqualizationItem(
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                "No Equalization",
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        leadingContent = { RadioButton(selected = isSelected, onClick = onSelected) },
        modifier = Modifier.clickable(onClick = onSelected).padding(horizontal = 8.dp)
    )
}

@Composable
private fun EQProfileItem(
    profile: SavedEQProfile,
    isSelected: Boolean,
    onSelected: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = {
            Text(
                profile.deviceModel,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        supportingContent = { Text("${profile.bands.size} band(s)") },
        leadingContent = { RadioButton(selected = isSelected, onClick = onSelected) },
        trailingContent = {
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        modifier = Modifier.clickable(onClick = onSelected).padding(horizontal = 8.dp)
    )

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Profile") },
            text = { Text("Delete ${profile.name}?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}
