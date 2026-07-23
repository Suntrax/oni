package com.blissless.oni

import android.content.Intent
import android.content.pm.ActivityInfo
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.blissless.oni.ui.screens.DownloadsScreen
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
            val useMaterial3Color by viewModel.useMaterial3Color.collectAsState()
            val monochromeTheme by viewModel.monochromeTheme.collectAsState()
            val oledTheme by viewModel.oledTheme.collectAsState()

            OniTheme(
                useMaterial3Color = useMaterial3Color,
                monochromeTheme = monochromeTheme,
                oledTheme = oledTheme
            ) {
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
    val lockRotation by viewModel.lockReaderRotation.collectAsState()

    var currentScreenType by rememberSaveable { mutableStateOf<String?>(null) }
    var currentNavRoute by rememberSaveable { mutableStateOf("home") }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    val currentScreen: Screen? = when (currentScreenType) {
        "detail" -> Screen.Detail
        "reader" -> Screen.Reader
        else -> null
    }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(currentNavRoute) {
        showSearch = false
    }
    LaunchedEffect(currentScreenType) {
        if (currentScreenType != null) showSearch = false
    }

    LaunchedEffect(lockRotation) {
        val activity = context as? MainActivity ?: return@LaunchedEffect
        activity.requestedOrientation = if (lockRotation) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    LaunchedEffect(showSearch) {
        if (!showSearch) {
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
                        currentScreenType = "detail"
                    },
                    onSearchClick = { showSearch = true },
                    onReadNow = { manga ->
                        viewModel.selectManga(manga)
                        currentScreenType = "detail"
                    }
                )
                "home" -> HomeScreen(
                    viewModel = viewModel,
                    onMangaSelected = { manga ->
                        viewModel.selectManga(manga)
                        currentScreenType = "detail"
                    },
                    onContinueReading = { track ->
                        viewModel.continueFromTracking(track) {
                            currentScreenType = "reader"
                        }
                    },
                    onResumeReading = { track ->
                        viewModel.resumeFromTracking(track) {
                            currentScreenType = "reader"
                        }
                    },
                    onRemoveResumeTracking = { track ->
                        viewModel.clearResumeProgress(track.mangaId)
                    },
                    onSearchClick = { showSearch = true },
                    onLoginClick = {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(viewModel.getAnilistAuthUrl())
                        )
                        context.startActivity(intent)
                    }
                )
                "downloads" -> DownloadsScreen()
                "settings" -> SettingsScreen(viewModel = viewModel)
            }
        }

        if (showSearch && currentScreen == null) {
            SearchScreen(
                viewModel = viewModel,
                onMangaSelected = { manga ->
                    viewModel.selectManga(manga)
                    currentScreenType = "detail"
                    showSearch = false
                },
                onDismiss = { showSearch = false },
                isActive = showSearch
            )
        }

        if (currentScreen != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (currentScreen) {
                    is Screen.Detail -> {
                        MangaDetailScreen(
                            viewModel = viewModel,
                            onBack = {
                                viewModel.refreshTrackingLists()
                                currentScreenType = null
                                viewModel.clearMangaDetail()
                            },
                            onOpenReader = {
                                currentScreenType = "reader"
                            },
                            onOpenReaderDirect = {
                                currentScreenType = "reader"
                            },
                            onOpenChapterSelect = {
                                currentScreenType = "reader"
                            }
                        )
                    }

                    is Screen.Reader -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            ReaderScreen(
                                viewModel = viewModel,
                                onBack = {
                                    currentScreenType = "detail"
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
