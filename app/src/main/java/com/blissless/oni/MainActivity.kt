package com.blissless.oni

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.blissless.oni.ui.components.BottomNavBar
import com.blissless.oni.ui.screens.ExploreScreen
import com.blissless.oni.ui.screens.HomeScreen
import com.blissless.oni.ui.screens.MangaDetailScreen
import com.blissless.oni.ui.screens.ReaderScreen
import com.blissless.oni.ui.screens.SearchScreen
import com.blissless.oni.ui.screens.SettingsScreen
import com.blissless.oni.ui.theme.OniTheme
import com.blissless.oni.viewmodel.MainViewModel
import com.blissless.oni.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(applicationContext)
    }
    private var isAniListAuthHandled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleAuthCallback(intent)
        setContent {
            OniTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OniApp(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthCallback(intent)
    }

    override fun onResume() {
        super.onResume()
        handleAuthCallback(intent)
    }

    private fun handleAuthCallback(intent: Intent?) {
        if (intent == null) return
        val uriString = intent.dataString ?: return
        if (!isAniListAuthHandled && uriString.startsWith("animescraper://success") && uriString.contains("access_token=")) {
            isAniListAuthHandled = true
            viewModel.handleAuthRedirect(intent)
        }
    }

    fun resetAuthFlags() {
        isAniListAuthHandled = false
    }
}

sealed class Screen {
    data object Detail : Screen()
    data object Reader : Screen()
}

@Composable
fun OniApp(viewModel: MainViewModel) {
    val context = LocalContext.current

    var currentScreen by remember { mutableStateOf<Screen?>(null) }
    var currentNavRoute by remember { mutableStateOf("home") }
    val keyboardController = LocalSoftwareKeyboardController.current



    LaunchedEffect(currentNavRoute) {
        if (currentNavRoute != "search") {
            keyboardController?.hide()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Crossfade(targetState = currentNavRoute, label = "nav") { route ->
            when (route) {
                "explore" -> ExploreScreen(
                    viewModel = viewModel,
                    onMangaSelected = { manga ->
                        viewModel.selectManga(manga)
                        currentScreen = Screen.Detail
                    }
                )
                "home" -> HomeScreen(
                    viewModel = viewModel,
                    onMangaSelected = { manga ->
                        viewModel.selectManga(manga)
                        currentScreen = Screen.Detail
                    },
                    onContinueReading = { track ->
                        viewModel.continueFromTracking(track) {
                            currentScreen = Screen.Reader
                        }
                    },
                    onResumeReading = { track ->
                        viewModel.resumeFromTracking(track) {
                            currentScreen = Screen.Reader
                        }
                    },
                    onRemoveResumeTracking = { track ->
                        viewModel.clearResumeProgress(track.mangaId)
                    }
                )
                "search" -> SearchScreen(
                    viewModel = viewModel,
                    onMangaSelected = { manga ->
                        viewModel.selectManga(manga)
                        currentScreen = Screen.Detail
                    },
                    isActive = currentNavRoute == "search"
                )
                "settings" -> SettingsScreen(viewModel = viewModel)
            }
        }

        if (currentScreen != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (currentScreen) {
                    is Screen.Detail -> {
                        MangaDetailScreen(
                            viewModel = viewModel,
                            onBack = {
                                viewModel.refreshTrackingLists()
                                currentScreen = null
                                viewModel.clearMangaDetail()
                            },
                            onOpenReader = {
                                currentScreen = Screen.Reader
                            },
                            onOpenReaderDirect = {
                                currentScreen = Screen.Reader
                            },
                            onOpenChapterSelect = {
                                currentScreen = Screen.Reader
                            }
                        )
                    }

                    is Screen.Reader -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            ReaderScreen(
                                viewModel = viewModel,
                                onBack = {
                                    currentScreen = Screen.Detail
                                    viewModel.clearSelection()
                                }
                            )
                        }
                    }

                    null -> {}
                }
            }
        }

        if (currentScreen == null) {
            BottomNavBar(
                currentRoute = currentNavRoute,
                onNavigate = { route -> currentNavRoute = route },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )
        }
    }
}
