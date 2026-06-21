package com.kuromusic.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import java.util.Calendar
import com.kuromusic.BuildConfig
import com.kuromusic.R
import com.kuromusic.constants.LastSeenVersionCodeKey
import com.kuromusic.constants.LanguageSelectorDismissedKey
import com.kuromusic.ui.component.LanguageSelector
import com.kuromusic.utils.rememberPreference

@Composable
fun WelcomeScreen(navController: NavController) {
    val context = LocalContext.current
    var currentPage by rememberSaveable { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(pageCount = { 3 }, initialPage = 0)
    val (langDismissed, onLangDismissedChange) = rememberPreference(LanguageSelectorDismissedKey, defaultValue = false)
    val (lastSeenVersionCode, onLastSeenVersionChange) = rememberPreference(LastSeenVersionCodeKey, defaultValue = 0)

    DisposableEffect(Unit) {
        onDispose {
            if (!langDismissed) {
                onLangDismissedChange(true)
            }
        }
    }

    LaunchedEffect(currentPage) {
        pagerState.animateScrollToPage(currentPage)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = false,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> WelcomeStep1()
                        1 -> WelcomeStep2()
                        2 -> WelcomePermissions()
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pagerState.pageCount) { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (pagerState.currentPage == index) 24.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == index)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentPage > 0) {
                    TextButton(onClick = {
                        if (currentPage > 0) currentPage--
                    }) {
                        Text(stringResource(R.string.welcome_back))
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        if (currentPage < pagerState.pageCount - 1) {
                            currentPage++
                        } else {
                            onLastSeenVersionChange(BuildConfig.VERSION_CODE)
                            navController.navigate(Screens.Home.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    inclusive = true
                                }
                            }
                        }
                    },
                    enabled = langDismissed,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        if (currentPage < pagerState.pageCount - 1)
                            stringResource(R.string.welcome_next)
                        else
                            stringResource(R.string.welcome_get_started)
                    )
                }
            }
        }

        if (!langDismissed) {
            LanguageSelector(
                onDismiss = {
                    onLangDismissedChange(true)
                }
            )
        }
    }
}

@Composable
private fun WelcomeStep1() {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val (greeting, emoji, gradientColors) = when (hour) {
        in 5..11 -> Triple("Buenos días", "☀️", listOf(Color(0xFFFF6B35), Color(0xFFFFB347)))
        in 12..17 -> Triple("Buenas tardes", "🌤️", listOf(Color(0xFF4A90D9), Color(0xFF67B8D9)))
        in 18..20 -> Triple("Buenas noches", "🌆", listOf(Color(0xFF2D1B69), Color(0xFF8B5CF6)))
        else -> Triple("Buenas noches", "🌙", listOf(Color(0xFF0F0C29), Color(0xFF302B63)))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = gradientColors,
                            start = Offset(0f, 0f),
                            end = Offset(1f, 1f)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$emoji $greeting",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Bienvenido a ",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                painter = painterResource(R.drawable.drawable_ic_kuro),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "KuroMusic",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Una forma distinta de escuchar\nla música que te gusta.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp
        )
    }
}

@Composable
private fun WelcomeBullet(icon: Int, title: String, subtitle: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WelcomeStep2() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.welcome_title_2),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.welcome_desc_2),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        WelcomeBullet(
            icon = R.drawable.lyrics,
            title = "Letras en vivo",
            subtitle = "Sincronizadas canción por canción"
        )
        Spacer(Modifier.height(16.dp))
        WelcomeBullet(
            icon = R.drawable.palette,
            title = "Tema inteligente",
            subtitle = "Los colores se adaptan al álbum que suena"
        )
        Spacer(Modifier.height(16.dp))
        WelcomeBullet(
            icon = R.drawable.tune,
            title = "Sonido a tu gusto",
            subtitle = "Ecualizador y normalización incluidos"
        )
    }
}

@Composable
private fun WelcomePermissions() {
    val context = LocalContext.current
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.welcome_title_3),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.welcome_desc_3),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        FilledTonalButton(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            enabled = !notifGranted,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                if (notifGranted) stringResource(R.string.welcome_permission_granted)
                else stringResource(R.string.welcome_permission_notif)
            )
        }
        Spacer(Modifier.height(8.dp))

        FilledTonalButton(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    storageLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
                } else {
                    storageLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            },
            enabled = !storageGranted,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                if (storageGranted) stringResource(R.string.welcome_permission_granted)
                else stringResource(R.string.welcome_permission_storage)
            )
        }
        Spacer(Modifier.height(8.dp))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            FilledTonalButton(
                onClick = {
                    val intent = android.content.Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    )
                    installLauncher.launch(intent)
                },
                enabled = !installGranted,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    if (installGranted) stringResource(R.string.welcome_permission_granted)
                    else "Instalar actualizaciones"
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        Text(
            text = stringResource(R.string.welcome_install_permission),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}
