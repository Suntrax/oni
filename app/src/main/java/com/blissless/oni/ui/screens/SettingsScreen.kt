package com.blissless.oni.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
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

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    var selectedGroup by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = selectedGroup != null) {
        selectedGroup = null
    }

    AnimatedContent(
        targetState = selectedGroup,
        transitionSpec = {
            if (targetState == null) {
                (fadeIn(animationSpec = tween(220)) + slideInHorizontally(animationSpec = tween(220)) { -it / 8 })
                    .togetherWith(fadeOut(animationSpec = tween(220)) + slideOutHorizontally(animationSpec = tween(220)) { it / 8 })
            } else {
                (fadeIn(animationSpec = tween(220)) + slideInHorizontally(animationSpec = tween(220)))
                    .togetherWith(fadeOut(animationSpec = tween(220)) + slideOutHorizontally(animationSpec = tween(220)))
            }
        },
        label = "settingsNavigation"
    ) { targetGroup ->
        if (targetGroup == null) {
            SettingsLandingPage(
                groups = listOf(
                    SettingsGroup("account", "Account & Sync", "AniList login and sync", Icons.Default.Person),
                    SettingsGroup("appearance", "Appearance", "Theme, colors, and display options", Icons.Default.Palette),
                    SettingsGroup("general", "General", "Startup screen and app behavior", Icons.Default.Settings),
                    SettingsGroup("reader", "Reader", "Reading mode and display", Icons.Default.Update),
                    SettingsGroup("extensions", "Extensions", "Manage manga source extensions", Icons.Default.Extension),
                    SettingsGroup("updates", "Updates & About", "App updates and version info", Icons.Default.Info),
                ),
                onGroupClick = { selectedGroup = it }
            )
        } else {
            BackHandler { selectedGroup = null }
            when (targetGroup) {
                "account" -> AccountSyncPage(viewModel = viewModel, onBack = { selectedGroup = null })
                "appearance" -> AppearancePage(viewModel = viewModel, onBack = { selectedGroup = null })
                "general" -> GeneralPage(viewModel = viewModel, onBack = { selectedGroup = null })
                "reader" -> ReaderPage(viewModel = viewModel, onBack = { selectedGroup = null })
                "extensions" -> ExtensionsPage(viewModel = viewModel, onBack = { selectedGroup = null })
                "updates" -> UpdatesAboutPage(viewModel = viewModel, onBack = { selectedGroup = null })
            }
        }
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
    val showPageIndicator by viewModel.showPageIndicator.collectAsState()

    SettingsPageScaffold(title = "Reader", onBack = onBack) {
        SettingsSectionHeader("READING MODE")
        SettingsCardGroup {
            ReaderMode.entries.forEachIndexed { index, mode ->
                SettingsRadioItem(
                    selected = readerMode == mode,
                    onClick = { viewModel.setReaderMode(mode) },
                    icon = mode.icon,
                    title = mode.displayLabel,
                    description = mode.description
                )
                if (index < ReaderMode.entries.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 54.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                        thickness = 0.5.dp
                    )
                }
            }
        }

        SettingsSectionHeader("DISPLAY")
        SettingsCardGroup {
            SettingsToggle(
                title = "Page Indicator",
                description = "Show current page number in the bottom-right corner while reading.",
                checked = showPageIndicator,
                onCheckedChange = { viewModel.setShowPageIndicator(it) }
            )
        }
    }
}

@Composable
private fun AppearancePage(viewModel: MainViewModel, onBack: () -> Unit) {
    val currentThemeMode by viewModel.themeMode.collectAsState()
    val monochromeTheme by viewModel.monochromeTheme.collectAsState()
    val lockReaderRotation by viewModel.lockReaderRotation.collectAsState()

    SettingsPageScaffold(title = "Appearance", onBack = onBack) {
        SettingsSectionHeader("THEME MODE")
        SettingsCardGroup {
            SettingsRadioItem(
                selected = currentThemeMode == com.blissless.oni.ui.theme.ThemeMode.SYSTEM,
                onClick = { viewModel.setThemeMode(com.blissless.oni.ui.theme.ThemeMode.SYSTEM) },
                icon = Icons.Default.Settings,
                title = "System Theme",
                description = "Follow your device theme setting"
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 54.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                thickness = 0.5.dp
            )
            SettingsRadioItem(
                selected = currentThemeMode == com.blissless.oni.ui.theme.ThemeMode.LIGHT,
                onClick = { viewModel.setThemeMode(com.blissless.oni.ui.theme.ThemeMode.LIGHT) },
                icon = Icons.Default.LightMode,
                title = "Light",
                description = "Bright and clean appearance"
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 54.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                thickness = 0.5.dp
            )
            SettingsRadioItem(
                selected = currentThemeMode == com.blissless.oni.ui.theme.ThemeMode.DARK,
                onClick = { viewModel.setThemeMode(com.blissless.oni.ui.theme.ThemeMode.DARK) },
                icon = Icons.Default.DarkMode,
                title = "Dark",
                description = "Easy on the eyes at night"
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 54.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                thickness = 0.5.dp
            )
            SettingsRadioItem(
                selected = currentThemeMode == com.blissless.oni.ui.theme.ThemeMode.OLED,
                onClick = { viewModel.setThemeMode(com.blissless.oni.ui.theme.ThemeMode.OLED) },
                icon = Icons.Default.Storage,
                title = "OLED",
                description = "Pure black for AMOLED screens"
            )
        }

        SettingsSectionHeader("COLORS")
        SettingsCardGroup {
            SettingsToggle(
                title = "Monochrome Theme",
                description = "Disable Material You colors for neutral appearance",
                checked = monochromeTheme,
                onCheckedChange = { viewModel.setMonochromeTheme(it) }
            )
        }

        SettingsSectionHeader("DISPLAY")
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
private fun GeneralPage(viewModel: MainViewModel, onBack: () -> Unit) {
    val startupScreen by viewModel.startupScreen.collectAsState()

    SettingsPageScaffold(title = "General", onBack = onBack) {
        SettingsSectionHeader("LAUNCH")
        SettingsCardGroup {
            SettingsRadioItem(
                selected = startupScreen == "home",
                onClick = { viewModel.setStartupScreen("home") },
                icon = Icons.Default.Home,
                title = "Home",
                description = "Your manga lists and reading progress"
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 54.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                thickness = 0.5.dp
            )
            SettingsRadioItem(
                selected = startupScreen == "explore",
                onClick = { viewModel.setStartupScreen("explore") },
                icon = Icons.Default.Explore,
                title = "Explore",
                description = "Browse and discover manga"
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 54.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                thickness = 0.5.dp
            )
            SettingsRadioItem(
                selected = startupScreen == "downloads",
                onClick = { viewModel.setStartupScreen("downloads") },
                icon = Icons.Default.FileDownload,
                title = "Downloads",
                description = "Downloaded chapters"
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
private fun extensionIconPainter(packageName: String): Painter? {
    val context = LocalContext.current
    val bitmap = remember(packageName) {
        try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            val drawable = context.packageManager.getApplicationIcon(appInfo)
            val bmp = android.graphics.Bitmap.createBitmap(
                drawable.intrinsicWidth.coerceAtLeast(1),
                drawable.intrinsicHeight.coerceAtLeast(1),
                android.graphics.Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bmp
        } catch (_: Exception) { null }
    }
    return bitmap?.let { BitmapPainter(it.asImageBitmap()) }
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
                            val painter = extensionIconPainter(ext.packageName)
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
                                    if (painter != null) {
                                        androidx.compose.foundation.Image(
                                            painter = painter,
                                            contentDescription = ext.label,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Widgets,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
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
