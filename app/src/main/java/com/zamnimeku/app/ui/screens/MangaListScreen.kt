package com.zamnimeku.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<MangaCard>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchMode by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun doSearch(query: String) {
        if (query.trim().isEmpty()) {
            searchMode = false
            searchResults = emptyList()
            return
        }
        searchMode = true
        scope.launch {
            isSearching = true
            try {
                val results = MangaApi.searchKomik(query.trim())
                searchResults = results
            } catch (_: Exception) {}
            finally {
                isSearching = false
            }
        }
    }

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
                color = WibukuBg,
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(WibukuPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MenuBook,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "BACA KOMIK",
                                style = MaterialTheme.typography.titleLarge,
                                color = WibukuText,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp,
                                fontSize = 20.sp
                            )
                            Text(
                                text = "Manga, Manhwa, & Manhua sub Indo",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WibukuMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Search Bar Pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                // 52dp: tinggi minimum TextField Material3 (±56dp)
                                // dikompensasi, teks tidak kepotong atas-bawah
                                .height(52.dp),
                            color = Color.White,
                            shape = RoundedCornerShape(12.dp),
                            shadowElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Search, contentDescription = null, tint = WibukuMuted, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                TextField(
                                    value = searchQuery,
                                    onValueChange = {
                                        searchQuery = it
                                        if (it.isEmpty()) {
                                            searchMode = false
                                            searchResults = emptyList()
                                        } else if (it.length >= 3) {
                                            doSearch(it)
                                        }
                                    },
                                    placeholder = { Text("Cari judul komik...", color = WibukuMuted, fontSize = 13.sp) },
                                    singleLine = true,
                                    // Samakan tinggi font input dengan placeholder
                                    // supaya tidak ketutup/terpotong di bar 52dp
                                    textStyle = TextStyle(
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        color = WibukuText
                                    ),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            searchQuery = ""
                                            searchMode = false
                                            searchResults = emptyList()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = WibukuMuted, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(WibukuPrimary)
                                .clickable { doSearch(searchQuery) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Search, contentDescription = "Search", tint = Color.White, modifier = Modifier.size(22.dp))
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
            if (searchMode) {
                if (isSearching) {
                    LoadingView(text = "Mencari komik '$searchQuery'...")
                } else if (searchResults.isNotEmpty()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(searchResults) { manga ->
                            MangaCardView(manga = manga) {
                                onMangaClick(manga.slug, manga.title, manga.thumb)
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Tidak ada hasil untuk '$searchQuery'", color = WibukuMuted)
                    }
                }
            } else {
                if (isLoading) {
                    LoadingView(text = "Memuat katalog komik...")
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
}
