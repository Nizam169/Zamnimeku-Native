package com.zamnimeku.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zamnimeku.app.data.api.OtakuApi
import com.zamnimeku.app.data.api.UpdateApi
import com.zamnimeku.app.data.api.UpdateInfo
import com.zamnimeku.app.data.model.Episode
import com.zamnimeku.app.data.storage.AppPreferences
import com.zamnimeku.app.ui.components.AppBottomBar
import com.zamnimeku.app.ui.components.LoadingView
import com.zamnimeku.app.ui.components.NavTab
import com.zamnimeku.app.ui.components.UpdateDialog
import com.zamnimeku.app.ui.screens.*
import com.zamnimeku.app.ui.theme.*
import kotlinx.coroutines.launch

sealed class Screen {
    object Main : Screen()
    data class AnimeDetail(val slug: String, val title: String, val thumb: String) : Screen()
    data class Player(
        val episodes: List<Episode>,
        val initialIndex: Int,
        val animeTitle: String,
        val animeSlug: String,
        val animeThumb: String
    ) : Screen()
    data class MangaDetail(val slug: String, val title: String, val thumb: String) : Screen()
    data class MangaReader(val chapterSlug: String, val chapterTitle: String) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZamnimekuTheme {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    var currentTab by remember { mutableStateOf(NavTab.HOME) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }
    val screenStack = remember { mutableStateListOf<Screen>() }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var checkingUpdate by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun runUpdateCheck(manual: Boolean) {
        if (checkingUpdate) return
        checkingUpdate = true
        scope.launch {
            try {
                val info = UpdateApi.checkUpdate()
                val installed = BuildConfig.VERSION_CODE
                when {
                    info == null -> {
                        if (manual) Toast.makeText(context, "Gagal cek update. Coba lagi.", Toast.LENGTH_SHORT).show()
                    }
                    info.versionCode > installed && info.tag != prefs.skippedUpdateTag -> {
                        updateInfo = info
                    }
                    manual -> {
                        // Reset skip supaya versi ini tetap bisa muncul lagi lain waktu
                        Toast.makeText(
                            context,
                            "Sudah versi terbaru (v${BuildConfig.VERSION_NAME}).",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (_: Exception) {
                if (manual) Toast.makeText(context, "Gagal cek update. Coba lagi.", Toast.LENGTH_SHORT).show()
            } finally {
                checkingUpdate = false
            }
        }
    }

    // Cek auto-update sekali tiap buka app: bandingkan versi terpasang
    // dengan release terbaru di GitHub.
    LaunchedEffect(Unit) {
        runUpdateCheck(manual = false)
    }

    if (updateInfo != null) {
        val info = updateInfo!!
        UpdateDialog(
            currentVersion = BuildConfig.VERSION_NAME,
            newVersion = info.versionName,
            notes = info.notes,
            tag = info.tag,
            apkUrl = info.apkUrl,
            onLater = {
                prefs.skippedUpdateTag = info.tag
                updateInfo = null
            }
        )
    }

    fun navigateTo(screen: Screen) {
        screenStack.add(currentScreen)
        currentScreen = screen
    }

    fun popBack() {
        if (screenStack.isNotEmpty()) {
            currentScreen = screenStack.removeLast()
        } else {
            currentScreen = Screen.Main
        }
    }

    BackHandler(enabled = currentScreen !is Screen.Main) {
        popBack()
    }

    Scaffold(
        bottomBar = {
            if (currentScreen is Screen.Main) {
                AppBottomBar(
                    currentTab = currentTab,
                    onTabSelected = { tab -> currentTab = tab }
                )
            }
        },
        containerColor = WibukuBg
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (currentScreen is Screen.Main) padding else PaddingValues(0.dp))
        ) {
            when (val screen = currentScreen) {
                is Screen.Main -> {
                    when (currentTab) {
                        NavTab.HOME -> HomeScreen(
                            onAnimeClick = { slug, title, thumb ->
                                navigateTo(Screen.AnimeDetail(slug, title, thumb))
                            },
                            onSearchClick = {},
                            onCheckUpdate = { runUpdateCheck(manual = true) }
                        )
                        NavTab.GENRE -> GenreScreen(
                            onAnimeClick = { slug, title, thumb ->
                                navigateTo(Screen.AnimeDetail(slug, title, thumb))
                            }
                        )
                        NavTab.SCHEDULE -> ScheduleScreen(
                            onAnimeClick = { slug, title, thumb ->
                                navigateTo(Screen.AnimeDetail(slug, title, thumb))
                            }
                        )
                        NavTab.MANGA -> MangaListScreen(
                            onMangaClick = { slug, title, thumb ->
                                navigateTo(Screen.MangaDetail(slug, title, thumb))
                            }
                        )
                        NavTab.HISTORY -> HistoryScreen(
                            onHistoryClick = { slug, title, thumb, _ ->
                                navigateTo(Screen.AnimeDetail(slug, title, thumb))
                            }
                        )
                    }
                }

                is Screen.AnimeDetail -> {
                    EpisodeDetailScreen(
                        animeSlug = screen.slug,
                        animeTitle = screen.title,
                        animeThumb = screen.thumb,
                        onBack = { popBack() },
                        onEpisodeClick = { epIndex, _ ->
                            navigateTo(
                                Screen.Player(
                                    episodes = emptyList(),
                                    initialIndex = epIndex,
                                    animeTitle = screen.title,
                                    animeSlug = screen.slug,
                                    animeThumb = screen.thumb
                                )
                            )
                        }
                    )
                }

                is Screen.Player -> {
                    var loadedEpisodes by remember { mutableStateOf(screen.episodes) }
                    var isFetchingEpisodes by remember { mutableStateOf(screen.episodes.isEmpty()) }

                    LaunchedEffect(screen.animeSlug) {
                        if (loadedEpisodes.isEmpty()) {
                            try {
                                val d = OtakuApi.getAnimeDetail(screen.animeSlug)
                                if (d != null) {
                                    loadedEpisodes = d.episodes
                                }
                            } catch (_: Exception) {}
                            finally {
                                isFetchingEpisodes = false
                            }
                        }
                    }

                    if (isFetchingEpisodes) {
                        LoadingView(text = "Menyiapkan episode...")
                    } else {
                        PlayerScreen(
                            episodes = loadedEpisodes,
                            initialIndex = screen.initialIndex,
                            animeTitle = screen.animeTitle,
                            animeSlug = screen.animeSlug,
                            animeThumb = screen.animeThumb,
                            onBack = { popBack() }
                        )
                    }
                }

                is Screen.MangaDetail -> {
                    MangaDetailScreen(
                        mangaSlug = screen.slug,
                        mangaTitle = screen.title,
                        mangaThumb = screen.thumb,
                        onBack = { popBack() },
                        onChapterClick = { chapterSlug, chapterTitle ->
                            navigateTo(Screen.MangaReader(chapterSlug, chapterTitle))
                        }
                    )
                }

                is Screen.MangaReader -> {
                    MangaReaderScreen(
                        chapterSlug = screen.chapterSlug,
                        chapterTitle = screen.chapterTitle,
                        onBack = { popBack() }
                    )
                }
            }
        }
    }
}
