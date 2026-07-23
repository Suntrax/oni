package com.blissless.oni.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blissless.oni.BuildConfig
import com.blissless.oni.MainActivity
import com.blissless.oni.data.ReaderMode
import com.blissless.oni.update.UpdateUiState
import com.blissless.oni.update.UpdateViewModel
import com.blissless.oni.viewmodel.MainViewModel
import kotlin.math.roundToInt

private sealed class SettingsNavRoute {
    data object Main : SettingsNavRoute()
    data object AccountSync : SettingsNavRoute()
    data object Reader : SettingsNavRoute()
    data object Appearance : SettingsNavRoute()
    data object UpdatesAbout : SettingsNavRoute()
    data object Extensions : SettingsNavRoute()
}

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    var currentRoute by remember { mutableStateOf<SettingsNavRoute>(SettingsNavRoute.Main) }

    BackHandler(enabled = currentRoute !is SettingsNavRoute.Main) {
        currentRoute = SettingsNavRoute.Main
    }

    AnimatedContent(
        targetState = currentRoute,
        transitionSpec = {
            if (targetState is SettingsNavRoute.Main) {
                slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
            } else {
                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
            }
        },
        label = "settings_nav"
    ) { route ->
        when (route) {
            is SettingsNavRoute.Main -> SettingsMainScreen(
                onNavigate = { currentRoute = it }
            )
            is SettingsNavRoute.AccountSync -> AccountSyncScreen(
                viewModel = viewModel,
                onBack = { currentRoute = SettingsNavRoute.Main }
            )
            is SettingsNavRoute.Reader -> ReaderSettingsScreen(
                viewModel = viewModel,
                onBack = { currentRoute = SettingsNavRoute.Main }
            )
            is SettingsNavRoute.Appearance -> AppearanceScreen(
                viewModel = viewModel,
                onBack = { currentRoute = SettingsNavRoute.Main }
            )
            is SettingsNavRoute.UpdatesAbout -> UpdatesAboutScreen(
                viewModel = viewModel,
                onBack = { currentRoute = SettingsNavRoute.Main }
            )
            is SettingsNavRoute.Extensions -> ExtensionsScreen(
                viewModel = viewModel,
                onBack = { currentRoute = SettingsNavRoute.Main }
            )
        }
    }
}

@Composable
private fun SettingsMainScreen(onNavigate: (SettingsNavRoute) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(48.dp))
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )
        Spacer(Modifier.height(8.dp))

        SettingsCard {
            SettingsNavItem(
                icon = Icons.Default.AccountCircle,
                title = "Account & Sync",
                subtitle = "AniList login and sync settings",
                tint = MaterialTheme.colorScheme.primary,
                trailing = {
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                },
                onClick = { onNavigate(SettingsNavRoute.AccountSync) }
            )
        }

        Spacer(Modifier.height(20.dp))

        SettingsCard {
            SettingsNavItem(
                icon = Icons.Default.Update,
                title = "Reader",
                subtitle = "Reading mode and rotation",
                tint = MaterialTheme.colorScheme.primary,
                trailing = {
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                },
                onClick = { onNavigate(SettingsNavRoute.Reader) }
            )
        }

        Spacer(Modifier.height(20.dp))

        SettingsCard {
            SettingsNavItem(
                icon = Icons.Default.Update,
                title = "Appearance",
                subtitle = "Theme and color settings",
                tint = MaterialTheme.colorScheme.primary,
                trailing = {
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                },
                onClick = { onNavigate(SettingsNavRoute.Appearance) }
            )
        }

        Spacer(Modifier.height(20.dp))

        SettingsCard {
            SettingsNavItem(
                icon = Icons.Default.Download,
                title = "Updates & About",
                subtitle = "App updates and version info",
                tint = MaterialTheme.colorScheme.primary,
                trailing = {
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                },
                onClick = { onNavigate(SettingsNavRoute.UpdatesAbout) }
            )
        }

        Spacer(Modifier.height(20.dp))

        SettingsCard {
            SettingsNavItem(
                icon = Icons.Default.Widgets,
                title = "Extensions",
                subtitle = "Manage manga source extensions",
                tint = MaterialTheme.colorScheme.primary,
                trailing = {
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                },
                onClick = { onNavigate(SettingsNavRoute.Extensions) }
            )
        }

        Spacer(Modifier.height(100.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountSyncScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val anilistUsername by viewModel.anilistUsername.collectAsState()
    val isSyncing by viewModel.isAniListSyncing.collectAsState()
    val syncThreshold by viewModel.anilistSyncThreshold.collectAsState()
    val showMergeDialog by viewModel.showMergeDialog.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.checkAnilistSession()
    }

    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account & Sync", color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))
            SettingsSectionHeader("Account")
            SettingsCard {
                if (anilistUsername != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Logged in as $anilistUsername", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("AniList Auth", color = MaterialTheme.colorScheme.primaryContainer, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        Text(
                            "Logout",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { showLogoutDialog = true }
                        )
                    }
                    SettingsDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Syncing...", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.syncAnilistManga() },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(6.dp))
                                Text("Sync Now", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                } else {
                    SettingsNavItem(
                        icon = Icons.Default.AccountCircle,
                        title = "AniList Account",
                        subtitle = "Log in to sync your list",
                        tint = MaterialTheme.colorScheme.primary,
                        trailing = {
                            Text("Login", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        },
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(viewModel.getAnilistAuthUrl()))
                            context.startActivity(intent)
                        }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            SettingsSectionHeader("Sync")
            SettingsCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto-sync Threshold", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
                        Text("$syncThreshold%", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Sync progress to AniList after reading this % of a chapter",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = syncThreshold.toFloat(),
                        onValueChange = { viewModel.updateAnilistSyncThreshold(it.roundToInt()) },
                        valueRange = 75f..100f,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            inactiveTickColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        ),
                        steps = 24
                    )
                }
            }

            Spacer(Modifier.height(100.dp))
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Logout", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to logout from AniList? Your locally synced manga will not be removed.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logoutAniList()
                        (context as? MainActivity)?.resetAuthFlags()
                        showLogoutDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Logout") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (showMergeDialog) {
        AlertDialog(
            onDismissRequest = { },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Local manga found", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = { Text("You have locally saved manga. How would you like to proceed?", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.overwriteAnilistWithLocal() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Overwrite AniList with local") }
                    Button(
                        onClick = { viewModel.discardLocalAndSync() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Discard local, use AniList") }
                    TextButton(
                        onClick = { viewModel.mergeLocalAndAnilist() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Merge \u2013 only add new entries", color = MaterialTheme.colorScheme.primary) }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderSettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val readerMode by viewModel.readerMode.collectAsState()
    val lockReaderRotation by viewModel.lockReaderRotation.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reader", color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))
            SettingsSectionHeader("Reading Mode")
            SettingsCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        "Choose how pages are laid out. Paged modes show one page per screen; vertical scrolls continuously.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(12.dp))

                    ReaderMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setReaderMode(mode) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (readerMode == mode) MaterialTheme.colorScheme.primary
                                        else Color.Transparent
                                    )
                                    .border(
                                        width = if (readerMode == mode) 0.dp else 1.dp,
                                        color = if (readerMode == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (readerMode == mode) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(mode.displayLabel, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(
                                    text = when (mode) {
                                        ReaderMode.VERTICAL_SCROLL -> "Webtoon-style continuous scroll"
                                        ReaderMode.LEFT_TO_RIGHT -> "One page per screen, swipe left for next"
                                        ReaderMode.RIGHT_TO_LEFT -> "One page per screen, swipe right for next (manga style)"
                                    },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SettingsSectionHeader("Rotation")
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Lock Rotation", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Lock app to portrait mode. Turn off to allow landscape.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = lockReaderRotation,
                        onCheckedChange = { viewModel.setLockReaderRotation(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onSurface,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val useMaterial3Color by viewModel.useMaterial3Color.collectAsState()
    val monochromeTheme by viewModel.monochromeTheme.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance", color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))
            SettingsSectionHeader("Theme")
            SettingsCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Material 3 Color", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Use wallpaper-based dynamic colors from your device theme.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = useMaterial3Color,
                            onCheckedChange = { viewModel.setMaterial3Color(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onSurface,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    SettingsDivider()
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Monochrome", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Grayscale theme without any accent colors.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = monochromeTheme,
                            onCheckedChange = { viewModel.setMonochromeTheme(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onSurface,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdatesAboutScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val checkUpdatesOnStart by viewModel.checkUpdatesOnStart.collectAsState()
    val pendingUpdate by viewModel.pendingUpdateRelease.collectAsState()
    val updateViewModel: UpdateViewModel = viewModel()
    val updateState by updateViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val githubUrl = "https://github.com/Suntrax/Oni"

    var showGitHubDialog by remember { mutableStateOf(false) }

    LaunchedEffect(pendingUpdate) {
        val release = pendingUpdate
        if (release != null && updateState.release == null && !updateState.isChecking) {
            updateViewModel.setRelease(release)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Updates & About", color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))
            SettingsSectionHeader("Updates")
            SettingsCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Check for Updates on Start", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = checkUpdatesOnStart,
                            onCheckedChange = { viewModel.setCheckUpdatesOnStart(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onSurface,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    SettingsDivider()
                    Spacer(Modifier.height(12.dp))
                    UpdateStatusSection(updateState, updateViewModel)
                }
            }

            Spacer(Modifier.height(20.dp))
            SettingsSectionHeader("About")
            SettingsCard {
                SettingsNavItem(
                    icon = Icons.Default.Info,
                    title = "Oni Manga Reader",
                    subtitle = "Version ${BuildConfig.VERSION_NAME}",
                    tint = MaterialTheme.colorScheme.primary,
                    trailing = {
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    },
                    onClick = { showGitHubDialog = true }
                )
            }

            Spacer(Modifier.height(100.dp))
        }
    }

    if (showGitHubDialog) {
        AlertDialog(
            onDismissRequest = { showGitHubDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Open GitHub", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = { Text("Open the GitHub page for Oni Manga Reader?", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        showGitHubDialog = false
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Open") }
            },
            dismissButton = {
                TextButton(onClick = { showGitHubDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtensionsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val extensions by viewModel.installedExtensions.collectAsState()
    val selectedExtensionAuthority by viewModel.selectedExtensionAuthority.collectAsState()

    var showExtensionsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.discoverExtensions()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Extensions", color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))
            SettingsSectionHeader("Installed Extensions")
            SettingsCard {
                SettingsNavItem(
                    icon = Icons.Default.Widgets,
                    title = "Extensions",
                    subtitle = run {
                        val selected = extensions.find { it.authority == selectedExtensionAuthority }
                        when {
                            selected != null -> selected.label
                            selectedExtensionAuthority != null -> "Selected extension"
                            extensions.isEmpty() -> "Tap to discover"
                            else -> "${extensions.size} extension(s) found"
                        }
                    },
                    tint = MaterialTheme.colorScheme.primary,
                    trailing = {
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    },
                    onClick = {
                        viewModel.discoverExtensions()
                        showExtensionsDialog = true
                    }
                )
            }

            Spacer(Modifier.height(100.dp))
        }
    }

    if (showExtensionsDialog) {
        AlertDialog(
            onDismissRequest = { showExtensionsDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Installed Extensions", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (extensions.isEmpty()) {
                        Text("No manga extensions installed.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Install an extension APK whose app label starts with \"Oni: \".",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Text("Tap an extension to select it:", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        extensions.forEachIndexed { index, ext ->
                            val isSelected = ext.authority == selectedExtensionAuthority
                            SettingsDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectExtension(if (isSelected) null else ext.authority)
                                        showExtensionsDialog = false
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (isSelected) Icons.Default.CheckCircle else Icons.Default.Widgets,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ext.label, color = if (isSelected) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                                    Text(ext.packageName, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                }
                                if (isSelected) {
                                    Text("Active", color = Color(0xFF10B981), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (index == extensions.lastIndex) {
                                SettingsDivider()
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        if (selectedExtensionAuthority != null) {
                            TextButton(
                                onClick = { viewModel.selectExtension(null); showExtensionsDialog = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Clear selection", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExtensionsDialog = false }) {
                    Text("Close", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}

@Composable
private fun UpdateStatusSection(state: UpdateUiState, viewModel: UpdateViewModel) {
    when {
        state.isChecking -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Checking for updates...", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        state.isDownloading -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Downloading update...", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        state.downloadedFile != null -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Download complete", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        state.error != null -> {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("${state.error}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
        state.release != null -> {
            val cleanTag = state.release.tagName.removePrefix("v").removePrefix("V")
            val currentVersion = BuildConfig.VERSION_NAME
            val parts1 = cleanTag.split(".").map { it.toIntOrNull() ?: 0 }
            val parts2 = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }
            val maxLen = maxOf(parts1.size, parts2.size)
            var cmp = 0
            for (i in 0 until maxLen) {
                val p1 = parts1.getOrElse(i) { 0 }
                val p2 = parts2.getOrElse(i) { 0 }
                if (p1 != p2) { cmp = p1 - p2; break }
            }
            if (cmp > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Update, contentDescription = null, tint = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("${state.release.tagName} available", color = MaterialTheme.colorScheme.primaryContainer, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { viewModel.downloadUpdate() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Download Update")
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Up to date (v$currentVersion)", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        else -> {
            Button(
                onClick = { viewModel.checkForUpdates() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Check for Updates", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

@Composable
fun SettingsNavItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color = Color.Unspecified,
    trailing: @Composable () -> Unit = {},
    onClick: () -> Unit
) {
    val resolvedTint = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(resolvedTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = resolvedTint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        trailing()
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
