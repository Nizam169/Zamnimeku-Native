package com.zamnimeku.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zamnimeku.app.data.api.OtakuApi
import com.zamnimeku.app.data.model.AnimeCard
import com.zamnimeku.app.data.model.Episode
import com.zamnimeku.app.ui.components.AnimeCardView
import com.zamnimeku.app.ui.components.AppBottomBar
import com.zamnimeku.app.ui.components.LoadingView
import com.zamnimeku.app.ui.components.NavTab
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
    data class MangaReader(val chapterSlug: String, val chapterTitle: String) : Screen()
    object Search : Screen()
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
    var currentTab by remember { mutableStateOf(NavTab.HOME) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }
    val screenStack = remember { mutableStateListOf<Screen>() }

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
                            onSearchClick = {
                                navigateTo(Screen.Search)
                            }
                        )
                        NavTab.GENRE -> GenreScreen(
                            onAnimeClick = { slug, title, thumb ->
                                navigateTo(Screen.AnimeDetail(slug, title, thumb))
                            }
                        )
                        NavTab.RANDOM -> RandomScreen(
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
                            onMangaClick = { slug, title, _ ->
                                navigateTo(Screen.MangaReader(slug, title))
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
                            // Fetch episodes and navigate to player
                            navigateTo(
                                Screen.Player(
                                    episodes = emptyList(), // will be loaded in player
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
                    val scope = rememberCoroutineScope()
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

                is Screen.MangaReader -> {
                    MangaReaderScreen(
                        chapterSlug = screen.chapterSlug,
                        chapterTitle = screen.chapterTitle,
                        onBack = { popBack() }
                    )
                }

                is Screen.Search -> {
                    SearchScreen(
                        onBack = { popBack() },
                        onAnimeClick = { slug, title, thumb ->
                            navigateTo(Screen.AnimeDetail(slug, title, thumb))
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onAnimeClick: (String, String, String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<AnimeCard>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun doSearch() {
        if (query.trim().isEmpty()) return
        scope.launch {
            isSearching = true
            try {
                val results = OtakuApi.searchAnime(query.trim())
                searchResults = results
            } catch (_: Exception) {}
            finally {
                isSearching = false
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = WibukuSurface,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Kembali")
                    }

                    TextField(
                        value = query,
                        onValueChange = {
                            query = it
                            if (it.length >= 3) doSearch()
                        },
                        placeholder = { Text("Cari judul anime...", color = WibukuMuted, fontSize = 14.sp) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    if (query.isNotEmpty()) {
                        IconButton(onClick = {
                            query = ""
                            searchResults = emptyList()
                        }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear")
                        }
                    } else {
                        IconButton(onClick = { doSearch() }) {
                            Icon(Icons.Rounded.Search, contentDescription = "Search", tint = WibukuPrimary)
                        }
                    }
                }
            }
        },
        containerColor = WibukuBg
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isSearching) {
                LoadingView(text = "Mencari anime '$query'...")
            } else if (searchResults.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(searchResults) { anime ->
                        AnimeCardView(anime = anime) {
                            onAnimeClick(anime.slug, anime.title, anime.thumb)
                        }
                    }
                }
            } else if (query.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tidak ada hasil untuk '$query'", color = WibukuMuted)
                }
            }
        }
    }
}
