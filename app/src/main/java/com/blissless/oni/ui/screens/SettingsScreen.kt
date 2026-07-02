package com.blissless.oni.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blissless.oni.BuildConfig
import com.blissless.oni.MainActivity
import com.blissless.oni.ui.theme.BlueAccent
import com.blissless.oni.ui.theme.BlueDark
import com.blissless.oni.ui.theme.BlueLight
import com.blissless.oni.ui.theme.DarkBackground
import com.blissless.oni.ui.theme.DarkElevated
import com.blissless.oni.ui.theme.DarkSurface
import com.blissless.oni.ui.theme.DarkSurfaceVariant
import com.blissless.oni.ui.theme.Silver
import com.blissless.oni.ui.theme.SilverDark
import com.blissless.oni.ui.theme.SilverLight
import com.blissless.oni.ui.theme.StatusDropped
import com.blissless.oni.update.UpdateUiState
import com.blissless.oni.update.UpdateViewModel
import com.blissless.oni.viewmodel.MainViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val anilistUsername by viewModel.anilistUsername.collectAsState()
    val isSyncing by viewModel.isAniListSyncing.collectAsState()
    val syncThreshold by viewModel.anilistSyncThreshold.collectAsState()
    val showMergeDialog by viewModel.showMergeDialog.collectAsState()
    val checkUpdatesOnStart by viewModel.checkUpdatesOnStart.collectAsState()
    val extensions by viewModel.installedExtensions.collectAsState()
    val selectedExtensionAuthority by viewModel.selectedExtensionAuthority.collectAsState()
    val context = LocalContext.current

    val updateViewModel: UpdateViewModel = viewModel()
    val updateState by updateViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkAnilistSession()
    }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showGitHubDialog by remember { mutableStateOf(false) }
    var showExtensionsDialog by remember { mutableStateOf(false) }
    val githubUrl = "https://github.com/Suntrax/Oni"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(48.dp))
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = SilverLight,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        Spacer(Modifier.height(8.dp))

        // Account Section
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
                            .background(BlueAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = BlueAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Logged in as $anilistUsername", color = SilverLight, style = MaterialTheme.typography.bodyLarge)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(BlueAccent.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("AniList Auth", color = BlueLight, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    Text(
                        "Logout",
                        color = StatusDropped,
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
                            color = BlueAccent,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Syncing...", color = SilverDark, style = MaterialTheme.typography.bodySmall)
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.syncAnilistManga() },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp), tint = BlueAccent)
                            Spacer(Modifier.width(6.dp))
                            Text("Sync Now", color = BlueAccent)
                        }
                    }
                }
            } else {
                SettingsNavItem(
                    icon = Icons.Default.AccountCircle,
                    title = "AniList Account",
                    subtitle = "Log in to sync your list",
                    tint = BlueAccent,
                    trailing = {
                        Text("Login", color = BlueAccent, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
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
                    Text("Auto-sync Threshold", color = SilverLight, style = MaterialTheme.typography.bodyLarge)
                    Text("$syncThreshold%", color = BlueAccent, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Sync progress to AniList after reading this % of a chapter",
                    color = SilverDark,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(4.dp))
                Slider(
                    value = syncThreshold.toFloat(),
                    onValueChange = { viewModel.updateAnilistSyncThreshold(it.roundToInt()) },
                    valueRange = 75f..100f,
                    steps = 24,
                    colors = SliderDefaults.colors(
                        thumbColor = BlueAccent,
                        activeTrackColor = BlueAccent,
                        inactiveTrackColor = DarkSurfaceVariant,
                        inactiveTickColor = BlueAccent
                    )
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Updates Section
        SettingsSectionHeader("Updates")
        SettingsCard {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Check for Updates on Start", color = SilverLight, style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = checkUpdatesOnStart,
                        onCheckedChange = { viewModel.setCheckUpdatesOnStart(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SilverLight,
                            checkedTrackColor = BlueAccent,
                            uncheckedThumbColor = SilverDark,
                            uncheckedTrackColor = DarkSurfaceVariant
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

        // Extensions Section
        SettingsSectionHeader("Extensions")
        SettingsCard {
            SettingsNavItem(
                icon = Icons.Default.Widgets,
                title = "Installed Extensions",
                subtitle = if (extensions.isEmpty()) "Tap to discover" else {
                    val selected = extensions.find { it.authority == selectedExtensionAuthority }
                    if (selected != null) selected.label else "${extensions.size} extension(s) found"
                },
                tint = BlueAccent,
                trailing = {
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = SilverDark, modifier = Modifier.size(16.dp))
                },
                onClick = {
                    viewModel.discoverExtensions()
                    showExtensionsDialog = true
                }
            )
        }

        Spacer(Modifier.height(20.dp))

        // About Section
        SettingsSectionHeader("About")
        SettingsCard {
            SettingsNavItem(
                icon = Icons.Default.Info,
                title = "Oni Manga Reader",
                subtitle = "Version ${BuildConfig.VERSION_NAME}",
                tint = BlueAccent,
                trailing = {
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = SilverDark, modifier = Modifier.size(16.dp))
                },
                onClick = { showGitHubDialog = true }
            )
        }

        Spacer(Modifier.height(100.dp))
    }

    if (showGitHubDialog) {
        AlertDialog(
            onDismissRequest = { showGitHubDialog = false },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp),
            title = { Text("Open GitHub", color = SilverLight, fontWeight = FontWeight.Bold) },
            text = { Text("Open the GitHub page for Oni Manga Reader?", color = SilverDark) },
            confirmButton = {
                Button(
                    onClick = {
                        showGitHubDialog = false
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Open") }
            },
            dismissButton = {
                TextButton(onClick = { showGitHubDialog = false }) {
                    Text("Cancel", color = SilverDark)
                }
            }
        )
    }

    if (showMergeDialog) {
        AlertDialog(
            onDismissRequest = { },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp),
            title = { Text("Local manga found", color = SilverLight, fontWeight = FontWeight.Bold) },
            text = { Text("You have locally saved manga. How would you like to proceed?", color = SilverDark) },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.overwriteAnilistWithLocal() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("Overwrite AniList with local") }
                    Button(
                        onClick = { viewModel.discardLocalAndSync() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusDropped),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("Discard local, use AniList") }
                    TextButton(
                        onClick = { viewModel.mergeLocalAndAnilist() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Merge \u2013 only add new entries", color = BlueAccent) }
                }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp),
            title = { Text("Logout", color = SilverLight, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to logout from AniList? Your locally synced manga will not be removed.", color = SilverDark) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logoutAniList()
                        (context as? MainActivity)?.resetAuthFlags()
                        showLogoutDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusDropped),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Logout") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = SilverDark)
                }
            }
        )
    }

    if (showExtensionsDialog) {
        AlertDialog(
            onDismissRequest = { showExtensionsDialog = false },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp),
            title = { Text("Installed Extensions", color = SilverLight, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (extensions.isEmpty()) {
                        Text("No manga extensions installed.", color = SilverDark, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Install an extension APK whose app label starts with \"Oni: \".",
                            color = SilverDark,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Text("Tap an extension to select it:", color = SilverDark, style = MaterialTheme.typography.bodySmall)
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
                                        .background(if (isSelected) BlueAccent.copy(alpha = 0.25f) else BlueAccent.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (isSelected) Icons.Default.CheckCircle else Icons.Default.Widgets,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFF10B981) else BlueAccent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ext.label, color = if (isSelected) Color(0xFF10B981) else SilverLight, style = MaterialTheme.typography.bodyMedium)
                                    Text(ext.packageName, color = SilverDark, style = MaterialTheme.typography.bodySmall)
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
                                Text("Clear selection", color = StatusDropped)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExtensionsDialog = false }) {
                    Text("Close", color = BlueAccent)
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
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BlueAccent, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Checking for updates...", color = SilverDark, style = MaterialTheme.typography.bodySmall)
            }
        }
        state.isDownloading -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BlueAccent, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Downloading update...", color = SilverDark, style = MaterialTheme.typography.bodySmall)
            }
        }
        state.downloadedFile != null -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Download complete", color = SilverDark, style = MaterialTheme.typography.bodySmall)
            }
        }
        state.error != null -> {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Error, contentDescription = null, tint = StatusDropped, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("${state.error}", color = StatusDropped, style = MaterialTheme.typography.bodySmall)
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
                    Icon(Icons.Default.Update, contentDescription = null, tint = BlueLight, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("v${state.release.tagName} available", color = BlueLight, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { viewModel.downloadUpdate() },
                    colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Download Update")
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Up to date (v$currentVersion)", color = SilverDark, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        else -> {
            Button(
                onClick = { viewModel.checkForUpdates() },
                colors = ButtonDefaults.buttonColors(containerColor = DarkElevated),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Check for Updates", color = SilverLight)
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = BlueAccent,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
    ) {
        content()
    }
}

@Composable
fun SettingsNavItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color = BlueAccent,
    trailing: @Composable () -> Unit = {},
    onClick: () -> Unit
) {
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
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = SilverLight, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, color = SilverDark, style = MaterialTheme.typography.bodySmall)
        }
        trailing()
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = SilverLight.copy(alpha = 0.06f)
    )
}
