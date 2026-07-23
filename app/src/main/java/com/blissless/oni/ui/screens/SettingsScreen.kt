package com.blissless.oni.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    when (currentRoute) {
        is SettingsNavRoute.Main -> SettingsLandingPage(
            groups = listOf(
                SettingsGroup("account", "Account & Sync", "AniList login and sync", Icons.Default.AccountCircle),
                SettingsGroup("reader", "Reader", "Reading mode and rotation", Icons.Default.Update),
                SettingsGroup("appearance", "Appearance", "Theme and color settings", Icons.Default.Settings),
                SettingsGroup("updates", "Updates & About", "App updates and version info", Icons.Default.Download),
                SettingsGroup("extensions", "Extensions", "Manage manga source extensions", Icons.Default.Widgets),
            ),
            onGroupClick = { id ->
                currentRoute = when (id) {
                    "account" -> SettingsNavRoute.AccountSync
                    "reader" -> SettingsNavRoute.Reader
                    "appearance" -> SettingsNavRoute.Appearance
                    "updates" -> SettingsNavRoute.UpdatesAbout
                    "extensions" -> SettingsNavRoute.Extensions
                    else -> SettingsNavRoute.Main
                }
            }
        )
        is SettingsNavRoute.AccountSync -> AccountSyncPage(
            viewModel = viewModel,
            onBack = { currentRoute = SettingsNavRoute.Main }
        )
        is SettingsNavRoute.Reader -> ReaderPage(
            viewModel = viewModel,
            onBack = { currentRoute = SettingsNavRoute.Main }
        )
        is SettingsNavRoute.Appearance -> AppearancePage(
            viewModel = viewModel,
            onBack = { currentRoute = SettingsNavRoute.Main }
        )
        is SettingsNavRoute.UpdatesAbout -> UpdatesAboutPage(
            viewModel = viewModel,
            onBack = { currentRoute = SettingsNavRoute.Main }
        )
        is SettingsNavRoute.Extensions -> ExtensionsPage(
            viewModel = viewModel,
            onBack = { currentRoute = SettingsNavRoute.Main }
        )
    }
}

@Composable
private fun AccountSyncPage(viewModel: MainViewModel, onBack: () -> Unit) {
    val anilistUsername by viewModel.anilistUsername.collectAsState()
    val isSyncing by viewModel.isAniListSyncing.collectAsState()
    val syncThreshold by viewModel.anilistSyncThreshold.collectAsState()
    val showMergeDialog by viewModel.showMergeDialog.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.checkAnilistSession()
    }

    var showLogoutDialog by remember { mutableStateOf(false) }

    SettingsPageScaffold(title = "Account & Sync", onBack = onBack) {
        SettingsSectionHeader("Account")
        SettingsCardGroup {
            if (anilistUsername != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Logged in as $anilistUsername", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    }
                    Text(
                        "Logout",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { showLogoutDialog = true }
                    )
                }
                if (isSyncing) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Syncing...", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    OutlinedButton(
                        onClick = { viewModel.syncAnilistManga() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sync Now", color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else {
                SettingsClickableRow(
                    icon = Icons.Default.AccountCircle,
                    title = "AniList Account",
                    subtitle = "Log in to sync your list",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(viewModel.getAnilistAuthUrl()))
                        context.startActivity(intent)
                    }
                )
            }
        }

        SettingsSectionHeader("Sync")
        SettingsCardGroup {
            SettingsSliderRow(
                title = "Auto-sync Threshold",
                description = "Sync progress to AniList after reading this % of a chapter",
                value = syncThreshold.toFloat(),
                valueRange = 75f..100f,
                valueLabel = "$syncThreshold%",
                onValueChange = { viewModel.updateAnilistSyncThreshold(it.roundToInt()) },
                minLabel = "75%",
                maxLabel = "100%"
            )
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Logout", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to logout from AniList?") },
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
            title = { Text("Local manga found", fontWeight = FontWeight.Bold) },
            text = { Text("You have locally saved manga. How would you like to proceed?") },
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
                    ) { Text("Merge - only add new entries", color = MaterialTheme.colorScheme.primary) }
                }
            }
        )
    }
}

@Composable
private fun ReaderPage(viewModel: MainViewModel, onBack: () -> Unit) {
    val readerMode by viewModel.readerMode.collectAsState()
    val lockReaderRotation by viewModel.lockReaderRotation.collectAsState()

    SettingsPageScaffold(title = "Reader", onBack = onBack) {
        SettingsSectionHeader("Reading Mode")
        SettingsCardGroup {
            Column {
                Text(
                    "Choose how pages are laid out.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                ReaderMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setReaderMode(mode) }
                            .padding(vertical = 8.dp),
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
                                .then(
                                    if (readerMode != mode) Modifier.background(Color.Transparent) else Modifier
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
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mode.displayLabel, fontWeight = FontWeight.Medium)
                            Text(
                                text = when (mode) {
                                    ReaderMode.VERTICAL_SCROLL -> "Webtoon-style continuous scroll"
                                    ReaderMode.LEFT_TO_RIGHT -> "One page per screen, swipe left"
                                    ReaderMode.RIGHT_TO_LEFT -> "One page per screen, swipe right (manga)"
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        SettingsSectionHeader("Rotation")
        SettingsCardGroup {
            SettingsToggle(
                title = "Lock Rotation",
                description = "Lock app to portrait mode. Turn off to allow landscape.",
                checked = lockReaderRotation,
                onCheckedChange = { viewModel.setLockReaderRotation(it) }
            )
        }
    }
}

@Composable
private fun AppearancePage(viewModel: MainViewModel, onBack: () -> Unit) {
    val useMaterial3Color by viewModel.useMaterial3Color.collectAsState()
    val monochromeTheme by viewModel.monochromeTheme.collectAsState()
    val oledTheme by viewModel.oledTheme.collectAsState()

    SettingsPageScaffold(title = "Appearance", onBack = onBack) {
        SettingsSectionHeader("Theme")
        SettingsCardGroup {
            SettingsToggle(
                title = "Material 3 Color",
                description = "Use wallpaper-based dynamic colors from your device theme.",
                checked = useMaterial3Color,
                onCheckedChange = { viewModel.setMaterial3Color(it) }
            )
            SettingsToggle(
                title = "Monochrome",
                description = "Grayscale theme without any accent colors.",
                checked = monochromeTheme,
                onCheckedChange = { viewModel.setMonochromeTheme(it) }
            )
            SettingsToggle(
                title = "Pure Black (OLED)",
                description = "True black background for OLED screens. Saves battery.",
                checked = oledTheme,
                onCheckedChange = { viewModel.setOledTheme(it) }
            )
        }
    }
}

@Composable
private fun UpdatesAboutPage(viewModel: MainViewModel, onBack: () -> Unit) {
    val checkUpdatesOnStart by viewModel.checkUpdatesOnStart.collectAsState()
    val pendingUpdate by viewModel.pendingUpdateRelease.collectAsState()
    val updateViewModel: UpdateViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
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

    SettingsPageScaffold(title = "Updates & About", onBack = onBack) {
        SettingsSectionHeader("Updates")
        SettingsCardGroup {
            SettingsToggle(
                title = "Check for Updates on Start",
                description = "Automatically check for new versions when the app launches.",
                checked = checkUpdatesOnStart,
                onCheckedChange = { viewModel.setCheckUpdatesOnStart(it) }
            )
            SettingsUpdateStatus(updateState, updateViewModel)
        }

        SettingsSectionHeader("About")
        SettingsCardGroup {
            SettingsClickableRow(
                icon = Icons.Default.Info,
                title = "Oni Manga Reader",
                subtitle = "Version ${BuildConfig.VERSION_NAME}",
                onClick = { showGitHubDialog = true }
            )
        }
    }

    if (showGitHubDialog) {
        AlertDialog(
            onDismissRequest = { showGitHubDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Open GitHub", fontWeight = FontWeight.Bold) },
            text = { Text("Open the GitHub page for Oni Manga Reader?") },
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

@Composable
private fun ExtensionsPage(viewModel: MainViewModel, onBack: () -> Unit) {
    val extensions by viewModel.installedExtensions.collectAsState()
    val selectedExtensionAuthority by viewModel.selectedExtensionAuthority.collectAsState()
    var showExtensionsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.discoverExtensions()
    }

    SettingsPageScaffold(title = "Extensions", onBack = onBack) {
        SettingsSectionHeader("Installed Extensions")
        SettingsCardGroup {
            SettingsClickableRow(
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
                onClick = {
                    viewModel.discoverExtensions()
                    showExtensionsDialog = true
                }
            )
        }
    }

    if (showExtensionsDialog) {
        AlertDialog(
            onDismissRequest = { showExtensionsDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Installed Extensions", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (extensions.isEmpty()) {
                        Text("No manga extensions installed.")
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Install an extension APK whose app label starts with \"Oni: \".",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text("Tap an extension to select it:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        extensions.forEach { ext ->
                            val isSelected = ext.authority == selectedExtensionAuthority
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
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
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (isSelected) Icons.Default.CheckCircle else Icons.Default.Widgets,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ext.label, color = if (isSelected) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                                    Text(ext.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (isSelected) {
                                    Text("Active", color = Color(0xFF10B981), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
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
private fun SettingsUpdateStatus(state: UpdateUiState, viewModel: UpdateViewModel) {
    when {
        state.isChecking -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Checking for updates...", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        state.isDownloading -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Downloading update...", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        state.downloadedFile != null -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Download complete", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        state.error != null -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${state.release.tagName} available", color = MaterialTheme.colorScheme.primaryContainer, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { viewModel.downloadUpdate() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Download Update")
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Up to date (v$currentVersion)", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        else -> {
            Button(
                onClick = { viewModel.checkForUpdates() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Check for Updates")
            }
        }
    }
}
