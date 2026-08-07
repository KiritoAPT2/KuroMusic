package com.kuromusic.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.kuromusic.BuildConfig
import com.kuromusic.constants.DarkModeKey
import com.kuromusic.constants.LanguageSelectorDismissedKey
import com.kuromusic.constants.LastSeenVersionCodeKey
import com.kuromusic.constants.PureBlackKey
import com.kuromusic.ui.component.LanguageSelector
import com.kuromusic.ui.component.LocaleManager
import com.kuromusic.ui.icons.BrokenIcon
import com.kuromusic.ui.icons.BrokenIcons
import com.kuromusic.ui.navigation.NavTab
import com.kuromusic.ui.screens.settings.DarkMode
import com.kuromusic.utils.rememberEnumPreference
import com.kuromusic.utils.rememberPreference
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(navController: NavController) {
    val context = LocalContext.current
    val (langDismissed, onLangDismissedChange) = rememberPreference(LanguageSelectorDismissedKey, defaultValue = false)
    val (lastSeenVersionCode, onLastSeenVersionChange) = rememberPreference(LastSeenVersionCodeKey, defaultValue = 0)
    val (darkMode, onDarkModeChange) = rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val (pureBlack, onPureBlackChange) = rememberPreference(PureBlackKey, defaultValue = false)

    val scope = rememberCoroutineScope()
    var showLanguageSheet by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            if (!langDismissed) {
                onLangDismissedChange(true)
            }
        }
    }

    var notifGranted by remember { mutableStateOf(false) }
    var storageGranted by remember { mutableStateOf(false) }
    var installGranted by remember { mutableStateOf(false) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notifGranted = granted }

    val storageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> storageGranted = granted }

    val installLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        installGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else true
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            storageGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_MEDIA_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            notifGranted = true
            storageGranted = true
        }
        installGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else true
    }

    val allPermissionsGranted = notifGranted && storageGranted && installGranted

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))

            Text(
                text = "KuroMusic",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Una forma distinta de escuchar\nla música que te gusta.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))

            SettingsSection(title = "Permisos") {
                SettingItem(
                    icon = BrokenIcons.notification,
                    title = "Notificaciones",
                    subtitle = "Controlar reproducción desde el panel",
                    enabled = !notifGranted,
                    onToggle = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
                ItemDivider()
                SettingItem(
                    icon = BrokenIcons.cloudConnection,
                    title = "Almacenamiento",
                    subtitle = "Guardar descargas y leer música local",
                    enabled = !storageGranted,
                    onToggle = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            storageLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
                        } else {
                            storageLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    },
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ItemDivider()
                    SettingItem(
                        icon = BrokenIcons.cloudAdd,
                        title = "Instalar actualizaciones",
                        subtitle = "Permitir instalación automática",
                        enabled = !installGranted,
                        onToggle = {
                            val intent = android.content.Intent(
                                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:${context.packageName}")
                            )
                            installLauncher.launch(intent)
                        },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            SettingsSection(title = "Apariencia") {
                ThemeSelector(
                    darkMode = darkMode,
                    onDarkModeChange = onDarkModeChange,
                    pureBlack = pureBlack,
                    onPureBlackChange = onPureBlackChange,
                )
            }

            Spacer(Modifier.height(16.dp))

            SettingsSection(title = "Idioma") {
                LanguageItem(
                    onClick = { showLanguageSheet = true }
                )
            }

            Spacer(Modifier.height(16.dp))

            SettingsSection(title = "Navegación") {
                TabConfigItem()
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Puedes cambiar estos ajustes más tarde\ndesde los Ajustes de la app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    onLastSeenVersionChange(BuildConfig.VERSION_CODE)
                    navController.navigate(Screens.Home.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(
                    text = "Comenzar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )

            Spacer(Modifier.height(24.dp))
            Spacer(Modifier.navigationBarsPadding())
        }
    }

    if (showLanguageSheet) {
        LanguageSelector(
            onDismiss = { showLanguageSheet = false }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingItem(
    icon: Int,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (!enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                    RoundedCornerShape(12.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            BrokenIcon(
                codePoint = icon,
                contentDescription = null,
                size = 20.dp,
                tint = if (!enabled) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }

        Switch(
            checked = !enabled,
            onCheckedChange = if (enabled) {{ onToggle() }} else null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )
    }
}

@Composable
private fun ThemeSelector(
    darkMode: DarkMode,
    onDarkModeChange: (DarkMode) -> Unit,
    pureBlack: Boolean,
    onPureBlackChange: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.animateContentSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val next = when (darkMode) {
                        DarkMode.AUTO -> DarkMode.ON
                        DarkMode.ON -> DarkMode.OFF
                        DarkMode.OFF -> DarkMode.AUTO
                    }
                    onDarkModeChange(next)
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                BrokenIcon(
                    codePoint = BrokenIcons.moon,
                    contentDescription = null,
                    size = 20.dp,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tema oscuro",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = when (darkMode) {
                        DarkMode.ON -> "Activado"
                        DarkMode.OFF -> "Desactivado"
                        DarkMode.AUTO -> "Seguir sistema"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            BrokenIcon(
                codePoint = when (darkMode) {
                    DarkMode.ON -> BrokenIcons.toggleOn
                    DarkMode.OFF -> BrokenIcons.toggleOff
                    DarkMode.AUTO -> BrokenIcons.setting
                },
                contentDescription = null,
                size = 20.dp,
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        if (darkMode == DarkMode.ON) {
            ItemDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPureBlackChange(!pureBlack) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHighest,
                            RoundedCornerShape(12.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    BrokenIcon(
                        codePoint = BrokenIcons.colorSwatch,
                        contentDescription = null,
                        size = 20.dp,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.width(14.dp))

                Text(
                    text = "Negro puro (AMOLED)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )

                Switch(
                    checked = pureBlack,
                    onCheckedChange = { onPureBlackChange(!pureBlack) },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }
    }
}

@Composable
private fun LanguageItem(onClick: () -> Unit) {
    val context = LocalContext.current
    val localeManager = remember { LocaleManager.getInstance(context) }
    val currentLanguage by localeManager.currentLanguage.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    RoundedCornerShape(12.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            BrokenIcon(
                codePoint = BrokenIcons.global,
                contentDescription = null,
                size = 20.dp,
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Idioma",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = currentLanguage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        BrokenIcon(
            codePoint = BrokenIcons.arrowRight3,
            contentDescription = null,
            size = 18.dp,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun TabConfigItem() {
    val tabs = remember { NavTab.defaults }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    RoundedCornerShape(12.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            BrokenIcon(
                codePoint = BrokenIcons.category2,
                contentDescription = null,
                size = 20.dp,
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Pestañas",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                tabs.forEach { tab ->
                    Text(
                        text = when (tab) {
                            NavTab.HOME -> "Inicio"
                            NavTab.EXPLORE -> "Explorar"
                            NavTab.LIBRARY -> "Biblioteca"
                            NavTab.OFFLINE -> "Offline"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                RoundedCornerShape(6.dp),
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 70.dp, end = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
    )
}
