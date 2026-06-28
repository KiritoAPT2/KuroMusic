package com.kuromusic.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import com.kuromusic.BuildConfig
import com.kuromusic.R
import com.kuromusic.constants.EnableLastFMScrobblingKey
import com.kuromusic.constants.LastFMSessionKey
import com.kuromusic.constants.LastFMUseNowPlaying
import com.kuromusic.constants.LastFMUsernameKey
import com.kuromusic.constants.ScrobbleDelayPercentKey
import com.kuromusic.constants.ScrobbleDelaySecondsKey
import com.kuromusic.constants.ScrobbleMinSongDurationKey
import com.kuromusic.lastfm.LastFM
import com.kuromusic.utils.dataStore
import com.kuromusic.utils.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LastFmSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior? = TopAppBarDefaults.enterAlwaysScrollBehavior(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val isLoggedIn = LastFM.sessionKey != null
    val username = context.dataStore.get(LastFMUsernameKey, "")
    var showLoginDialog by remember { mutableStateOf(false) }
    var showScrobbleDelayDialog by remember { mutableStateOf(false) }
    var showMinDurationDialog by remember { mutableStateOf(false) }
    var showDelaySecondsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Last.fm") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            StatusHeader(isLoggedIn, username)

            Spacer(Modifier.height(24.dp))

            if (!isLoggedIn) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { showLoginDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(0.7f).height(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.lastfm),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Conectar con Last.fm", fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            context.dataStore.edit { prefs ->
                                prefs[LastFMSessionKey] = ""
                                prefs[LastFMUsernameKey] = ""
                                prefs[EnableLastFMScrobblingKey] = false
                            }
                            LastFM.sessionKey = null
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.logout),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Cerrar sesión", fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text(
                "Scrobbleo",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))

            val scrobblingEnabled = context.dataStore.get(EnableLastFMScrobblingKey, false)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Scrobbleo automático", fontWeight = FontWeight.Medium)
                    Text(
                        "Enviar canciones escuchadas a Last.fm",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = scrobblingEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            context.dataStore.edit { prefs ->
                                prefs[EnableLastFMScrobblingKey] = enabled
                            }
                        }
                    },
                    colors = androidx.compose.material3.SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            if (scrobblingEnabled) {
                Spacer(Modifier.height(8.dp))

                val nowPlayingEnabled = context.dataStore.get(LastFMUseNowPlaying, false)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Now Playing", fontWeight = FontWeight.Medium)
                        Text(
                            if (nowPlayingEnabled) "Mostrar estado en Last.fm"
                            else "Solo scrobblear al alcanzar el umbral",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = nowPlayingEnabled,
                        onCheckedChange = {
                            scope.launch {
                                context.dataStore.edit { prefs ->
                                    prefs[LastFMUseNowPlaying] = it
                                }
                            }
                        },
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text(
                "Umbral de scrobbleo",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))

            ThresholdItem(
                title = "Porcentaje de reproducción",
                value = "${(context.dataStore.get(ScrobbleDelayPercentKey, 0.5f) * 100).toInt()}%",
                description = "de la duración de la canción",
                onClick = { showScrobbleDelayDialog = true }
            )
            ThresholdItem(
                title = "Duración mínima",
                value = "${context.dataStore.get(ScrobbleMinSongDurationKey, 30)}s",
                description = "la canción debe durar al menos esto",
                onClick = { showMinDurationDialog = true }
            )
            ThresholdItem(
                title = "Tiempo máximo de espera",
                value = "${context.dataStore.get(ScrobbleDelaySecondsKey, 180)}s",
                description = "para hacer scrobble aunque no se alcance el %",
                onClick = { showDelaySecondsDialog = true }
            )

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showLoginDialog) {
        LoginDialog(
            onDismiss = { showLoginDialog = false },
            onLoggedIn = { name, session ->
                scope.launch {
                    context.dataStore.edit { prefs ->
                        prefs[LastFMSessionKey] = session
                        prefs[LastFMUsernameKey] = name
                        prefs[EnableLastFMScrobblingKey] = true
                    }
                    LastFM.sessionKey = session
                }
                showLoginDialog = false
            }
        )
    }

    if (showScrobbleDelayDialog) {
        val currentVal = context.dataStore.get(ScrobbleDelayPercentKey, 0.5f)
        var sliderVal by remember { mutableFloatStateOf(currentVal) }
        AlertDialog(
            onDismissRequest = { showScrobbleDelayDialog = false },
            title = { Text("Porcentaje de reproducción") },
            text = {
                Column {
                    Text("¿Qué porcentaje de la canción debe reproducirse antes de scrobblear?")
                    Spacer(Modifier.height(16.dp))
                    Slider(
                        value = sliderVal,
                        onValueChange = { sliderVal = it },
                        valueRange = 0.1f..1.0f,
                        steps = 8
                    )
                    Text(
                        "${(sliderVal * 100).toInt()}%",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        context.dataStore.edit { prefs ->
                            prefs[ScrobbleDelayPercentKey] = sliderVal
                        }
                    }
                    showScrobbleDelayDialog = false
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showScrobbleDelayDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showMinDurationDialog) {
        val currentVal = context.dataStore.get(ScrobbleMinSongDurationKey, 30)
        var sliderVal by remember { mutableFloatStateOf(currentVal.toFloat()) }
        AlertDialog(
            onDismissRequest = { showMinDurationDialog = false },
            title = { Text("Duración mínima") },
            text = {
                Column {
                    Text("Ignorar canciones más cortas que esto:")
                    Spacer(Modifier.height(16.dp))
                    Slider(
                        value = sliderVal,
                        onValueChange = { sliderVal = it },
                        valueRange = 10f..300f,
                        steps = 28
                    )
                    Text(
                        "${sliderVal.toInt()}s",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        context.dataStore.edit { prefs ->
                            prefs[ScrobbleMinSongDurationKey] = sliderVal.toInt()
                        }
                    }
                    showMinDurationDialog = false
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showMinDurationDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showDelaySecondsDialog) {
        val currentVal = context.dataStore.get(ScrobbleDelaySecondsKey, 180)
        var sliderVal by remember { mutableFloatStateOf(currentVal.toFloat()) }
        AlertDialog(
            onDismissRequest = { showDelaySecondsDialog = false },
            title = { Text("Tiempo máximo") },
            text = {
                Column {
                    Text("Scrobblear después de este tiempo aunque no se alcance el porcentaje:")
                    Spacer(Modifier.height(16.dp))
                    Slider(
                        value = sliderVal,
                        onValueChange = { sliderVal = it },
                        valueRange = 30f..600f,
                        steps = 18
                    )
                    Text(
                        "${sliderVal.toInt()}s",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        context.dataStore.edit { prefs ->
                            prefs[ScrobbleDelaySecondsKey] = sliderVal.toInt()
                        }
                    }
                    showDelaySecondsDialog = false
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showDelaySecondsDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun StatusHeader(isLoggedIn: Boolean, username: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isLoggedIn) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isLoggedIn) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.lastfm),
                contentDescription = null,
                tint = if (isLoggedIn) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                if (isLoggedIn) "Conectado" else "Desconectado",
                fontWeight = FontWeight.Bold,
                color = if (isLoggedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                if (isLoggedIn) username else "No has iniciado sesión",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(
                    if (isLoggedIn) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant
                )
        )
    }
}

@Composable
private fun ThresholdItem(
    title: String,
    value: String,
    description: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.history),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            value,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            painter = painterResource(R.drawable.chevron_right),
            contentDescription = "Ajustar",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun LoginDialog(
    onDismiss: () -> Unit,
    onLoggedIn: (String, String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isLoading,
            dismissOnClickOutside = !isLoading,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.lastfm),
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Last.fm",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Inicia sesión para scrobblear tu música",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(20.dp))

                if (error != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; error = null },
                    label = { Text("Usuario") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    label = { Text("Contraseña") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (!isLoading && username.isNotBlank() && password.isNotBlank()) {
                                isLoading = true
                                scope.launch { doLogin(username, password, onLoggedIn, { error = it }, { isLoading = false }) }
                            }
                        }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                painter = if (passwordVisible) painterResource(R.drawable.visibility_off) else painterResource(R.drawable.visibility),
                                contentDescription = if (passwordVisible) "Ocultar" else "Mostrar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (username.isBlank() || password.isBlank()) {
                            error = "Completa ambos campos"
                            return@Button
                        }
                        isLoading = true
                        error = null
                        scope.launch {
                            doLogin(username, password, onLoggedIn, { error = it }, { isLoading = false })
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Conectando...")
                    } else {
                        Text("Iniciar sesión", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private suspend fun doLogin(
    username: String,
    password: String,
    onLoggedIn: (String, String) -> Unit,
    onError: (String) -> Unit,
    onDone: () -> Unit,
) {
    val result = withContext(Dispatchers.IO) {
        LastFM.getMobileSession(
            username = username,
            password = password,
            apiKey = BuildConfig.LASTFM_API_KEY,
            secret = BuildConfig.LASTFM_SECRET
        )
    }
    result.onSuccess { auth ->
        onLoggedIn(auth.session.name, auth.session.key)
    }.onFailure { e ->
        val msg = e.message ?: "Desconocido"
        onError(
            when {
                msg.contains("Invalid password", ignoreCase = true) ->
                    "Contraseña incorrecta"
                msg.contains("User not found", ignoreCase = true) ||
                msg.contains("does not exist", ignoreCase = true) ->
                    "Usuario no encontrado"
                msg.contains("API", ignoreCase = true) ||
                msg.contains("key", ignoreCase = true) ->
                    "Error de configuración (API key)"
                else -> "Error: $msg"
            }
        )
        onDone()
    }
}
