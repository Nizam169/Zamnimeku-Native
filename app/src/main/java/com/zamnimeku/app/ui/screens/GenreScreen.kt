package com.zamnimeku.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zamnimeku.app.data.api.AnimeApi
import com.zamnimeku.app.data.api.OtakuApi
import com.zamnimeku.app.data.model.AnimeCard
import com.zamnimeku.app.data.model.AnimeSource
import com.zamnimeku.app.data.model.Genre
import com.zamnimeku.app.ui.components.AnimeCardView
import com.zamnimeku.app.ui.components.ErrorView
import com.zamnimeku.app.ui.components.LoadingView
import com.zamnimeku.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenreScreen(
    onAnimeClick: (String, String, String) -> Unit,
    source: AnimeSource = AnimeSource.OTAKUDESU
) {
    val useMyNimeku = source == AnimeSource.MYNIMEKU
    val scope = rememberCoroutineScope()
    var genres by remember { mutableStateOf<List<Genre>>(emptyList()) }
    var selectedGenre by remember { mutableStateOf<Genre?>(null) }

    var animeList by remember { mutableStateOf<List<AnimeCard>>(emptyList()) }
    var page by remember { mutableStateOf(1) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingAnime by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadAnimeForGenre(genre: Genre, initial: Boolean = false) {
        scope.launch {
            if (initial) {
                isLoadingAnime = true
                errorMessage = null
            } else {
                isLoadingMore = true
            }
            try {
                val p = if (initial) 1 else page + 1
                val items = if (useMyNimeku) {
                    AnimeApi.getAnimeByGenre(genre.slug, p)
                } else {
                    OtakuApi.getAnimeByGenre(genre.slug, p)
                }
                if (initial) {
                    animeList = items
                    page = 1
                } else {
                    val combined = (animeList + items).distinctBy { it.slug }
                    animeList = combined
                    page = p
                }
            } catch (e: Exception) {
                if (initial) errorMessage = "Gagal memuat anime: ${e.message}"
            } finally {
                isLoadingAnime = false
                isLoadingMore = false
            }
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val list = if (useMyNimeku) {
                AnimeApi.getGenreList()
            } else {
                OtakuApi.getGenreList()
            }
            genres = list
            if (list.isNotEmpty()) {
                selectedGenre = list.first()
                loadAnimeForGenre(list.first(), initial = true)
            }
        } catch (e: Exception) {
            errorMessage = "Gagal memuat daftar genre: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = WibukuSurface,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Text(
                        text = "Kategori & Genre",
                        style = MaterialTheme.typography.titleLarge,
                        color = WibukuText,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
                    )

                    if (genres.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(genres) { g ->
                                val isSelected = g.slug == selectedGenre?.slug
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable {
                                            if (selectedGenre?.slug != g.slug) {
                                                selectedGenre = g
                                                loadAnimeForGenre(g, initial = true)
                                            }
                                        },
                                    color = if (isSelected) WibukuPrimary else WibukuBadge,
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text(
                                        text = g.name,
                                        color = if (isSelected) Color.White else WibukuText,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                    )
                                }
                            }
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
            if (isLoading || isLoadingAnime) {
                LoadingView()
            } else if (errorMessage != null && animeList.isEmpty()) {
                ErrorView(
                    message = errorMessage ?: "Terjadi kesalahan",
                    onRetry = { selectedGenre?.let { loadAnimeForGenre(it, initial = true) } }
                )
            } else {
                val gridState = rememberLazyGridState()

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = gridState,
                    contentPadding = PaddingValues(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(animeList) { anime ->
                        AnimeCardView(anime = anime) {
                            onAnimeClick(anime.slug, anime.title, anime.thumb)
                        }
                    }

                    if (isLoadingMore) {
                        item(span = { GridItemSpan(3) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = WibukuPrimary, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }

                // Infinite scroll listener
                val shouldLoadMore = remember {
                    derivedStateOf {
                        val layoutInfo = gridState.layoutInfo
                        val totalItems = layoutInfo.totalItemsCount
                        val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        totalItems > 0 && lastVisible >= totalItems - 6
                    }
                }

                LaunchedEffect(shouldLoadMore.value) {
                    val g = selectedGenre
                    if (shouldLoadMore.value && !isLoadingMore && !isLoadingAnime && g != null) {
                        loadAnimeForGenre(g, initial = false)
                    }
                }
            }
        }
    }
}
