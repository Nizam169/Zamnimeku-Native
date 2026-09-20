package com.zamnimeku.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zamnimeku.app.data.api.MangaApi
import com.zamnimeku.app.data.model.MangaCard
import com.zamnimeku.app.ui.components.ErrorView
import com.zamnimeku.app.ui.components.LoadingView
import com.zamnimeku.app.ui.components.MangaCardView
import com.zamnimeku.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun MangaListScreen(
    onMangaClick: (String, String, String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var mangaList by remember { mutableStateOf<List<MangaCard>>(emptyList()) }
    var page by remember { mutableStateOf(1) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadManga(initial: Boolean = false) {
        scope.launch {
            if (initial) {
                isLoading = true
                errorMessage = null
            } else {
                isLoadingMore = true
            }
            try {
                val p = if (initial) 1 else page + 1
                val items = MangaApi.listKomik(page = p)
                if (initial) {
                    mangaList = items
                    page = 1
                } else {
                    val combined = (mangaList + items).distinctBy { it.slug }
                    mangaList = combined
                    page = p
                }
            } catch (e: Exception) {
                if (initial) errorMessage = "Gagal memuat komik: ${e.message}"
            } finally {
                isLoading = false
                isLoadingMore = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadManga(initial = true)
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
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Baca Komik & Manga",
                        style = MaterialTheme.typography.titleLarge,
                        color = WibukuText
                    )
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
            if (isLoading) {
                LoadingView(text = "Memuat daftar komik...")
            } else if (errorMessage != null && mangaList.isEmpty()) {
                ErrorView(message = errorMessage ?: "Terjadi kesalahan", onRetry = { loadManga(initial = true) })
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
                    items(mangaList) { manga ->
                        MangaCardView(manga = manga) {
                            onMangaClick(manga.slug, manga.title, manga.thumb)
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

                val shouldLoadMore = remember {
                    derivedStateOf {
                        val layoutInfo = gridState.layoutInfo
                        val totalItems = layoutInfo.totalItemsCount
                        val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        totalItems > 0 && lastVisible >= totalItems - 6
                    }
                }

                LaunchedEffect(shouldLoadMore.value) {
                    if (shouldLoadMore.value && !isLoadingMore && !isLoading) {
                        loadManga(initial = false)
                    }
                }
            }
        }
    }
}
