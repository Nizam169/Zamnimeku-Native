package com.zamnimeku.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zamnimeku.app.data.api.AnimeApi
import com.zamnimeku.app.data.api.OtakuApi
import com.zamnimeku.app.data.model.AnimeCard
import com.zamnimeku.app.data.model.AnimeSource
import com.zamnimeku.app.ui.components.AnimeCardView
import com.zamnimeku.app.ui.components.ErrorView
import com.zamnimeku.app.ui.components.LoadingView
import com.zamnimeku.app.ui.components.ContinueWatchingCard
import com.zamnimeku.app.ui.components.PosterImage
import com.zamnimeku.app.ui.components.SectionHeader
import com.zamnimeku.app.ui.components.StatCard
import com.zamnimeku.app.ui.components.HeroCarousel
import com.zamnimeku.app.ui.components.PosterRail
import com.zamnimeku.app.ui.components.StaggeredItem
import com.zamnimeku.app.ui.components.PosterSkeletonGrid
import com.zamnimeku.app.ui.components.PulseDot
import com.zamnimeku.app.data.storage.AppPreferences
import com.zamnimeku.app.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAnimeClick: (String, String, String) -> Unit,
    onSearchClick: () -> Unit,
    onCheckUpdate: () -> Unit = {},
    source: AnimeSource = AnimeSource.OTAKUDESU
) {
    val useMyNimeku = source == AnimeSource.MYNIMEKU
    val context = androidx.compose.ui.platform.LocalContext.current
    val history = remember { AppPreferences(context).getHistory() }
    var selectedTab by remember { mutableStateOf(0) }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var loadedSource by remember { mutableStateOf(source) }
    val scope = rememberCoroutineScope()

    var ongoingList by remember { mutableStateOf<List<AnimeCard>>(emptyList()) }
    var completeList by remember { mutableStateOf<List<AnimeCard>>(emptyList()) }
    var ongoingPage by remember { mutableStateOf(1) }
    var completePage by remember { mutableStateOf(1) }

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<AnimeCard>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchMode by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var requestedPageAtEnd by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun doSearch(query: String) {
        if (query.trim().isEmpty()) {
            searchMode = false
            searchResults = emptyList()
            return
        }
        searchMode = true
        searchJob?.cancel()
        searchJob = scope.launch {
            isSearching = true
            try {
                val results = if (useMyNimeku) {
                    AnimeApi.searchAnime(query.trim())
                } else {
                    OtakuApi.searchAnime(query.trim())
                }
                if (searchQuery.trim() == query.trim()) searchResults = results
            } catch (_: Exception) {
                if (searchQuery.trim() == query.trim()) searchResults = emptyList()
            }
            finally {
                if (searchQuery.trim() == query.trim()) isSearching = false
            }
        }
    }

    fun loadData(initial: Boolean = false) {
        scope.launch {
            if (initial) {
                isLoading = true
                errorMessage = null
            } else {
                isLoadingMore = true
            }
            try {
                if (selectedTab == 0) {
                    val p = if (initial) 1 else ongoingPage + 1
                    val items = if (useMyNimeku) {
                        AnimeApi.getOngoingAnime(p)
                    } else {
                        OtakuApi.getOngoingAnime(p)
                    }
                    if (initial) {
                        ongoingList = items
                        ongoingPage = 1
                    } else {
                        val combined = (ongoingList + items).distinctBy { it.slug }
                        ongoingList = combined
                        ongoingPage = p
                    }
                } else {
                    val p = if (initial) 1 else completePage + 1
                    val items = if (useMyNimeku) {
                        AnimeApi.getCompleteAnime(p)
                    } else {
                        OtakuApi.getCompleteAnime(p)
                    }
                    if (initial) {
                        completeList = items
                        completePage = 1
                    } else {
                        val combined = (completeList + items).distinctBy { it.slug }
                        completeList = combined
                        completePage = p
                    }
                }
            } catch (e: Exception) {
                if (initial) errorMessage = "Gagal memuat data: ${e.message}"
            } finally {
                isLoading = false
                isLoadingMore = false
            }
        }
    }

    LaunchedEffect(selectedTab, source) {
        searchJob?.cancel()
        isSearching = false
        if (loadedSource != source) {
            ongoingList = emptyList()
            completeList = emptyList()
            ongoingPage = 1
            completePage = 1
            searchResults = emptyList()
            searchMode = false
            loadedSource = source
        }
        if (selectedTab == 0 && ongoingList.isEmpty()) {
            loadData(initial = true)
        } else if (selectedTab == 1 && completeList.isEmpty()) {
            loadData(initial = true)
        }
    }

    // Debounce typing and discard the previous query when a new one arrives.
    LaunchedEffect(searchQuery, source) {
        if (searchQuery.length >= 3) {
            delay(350)
            doSearch(searchQuery)
        }
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
                    // Editorial app header.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 16.dp, top = 18.dp, bottom = 8.dp),
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
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Halo, Otaku",
                                style = MaterialTheme.typography.headlineMedium,
                                color = WibukuText,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp,
                                fontSize = 20.sp
                            )
                            Text(
                                text = if (useMyNimeku) "Senang melihatmu kembali · Anime2" else "Siap untuk episode berikutnya?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WibukuMuted,
                                fontSize = 12.sp
                            )
                        }

                        // Tombol cek pembaruan manual
                        IconButton(onClick = onCheckUpdate) {
                            Icon(
                                imageVector = Icons.Rounded.SystemUpdate,
                                contentDescription = "Cek Pembaruan",
                                tint = WibukuMuted,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // ── SEARCH BAR PILL ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            color = WibukuCard,
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
                                            searchJob?.cancel()
                                            isSearching = false
                                            searchMode = false
                                            searchResults = emptyList()
                                        } else if (it.length < 3) {
                                            searchJob?.cancel()
                                            isSearching = false
                                            searchMode = false
                                            searchResults = emptyList()
                                        }
                                    },
                                    placeholder = { Text("Cari anime...", color = WibukuMuted, fontSize = 13.sp) },
                                    singleLine = true,
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
                                            searchJob?.cancel()
                                            isSearching = false
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

                    // ── TAB SELECTOR (ONGOING / COMPLETE) ──
                    if (!searchMode) {
                        com.zamnimeku.app.ui.components.SegmentedTabs(
                            labels = listOf("Ongoing", "Complete"),
                            selectedIndex = selectedTab,
                            onSelected = { selectedTab = it },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    if (useMyNimeku) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Semua", "Anime", "Donghua", "Hentai").forEach { category ->
                                Surface(
                                    modifier = Modifier.clickable { selectedCategory = category },
                                    color = if (selectedCategory == category) WibukuPrimary else WibukuCard,
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Text(
                                        text = category,
                                        color = if (selectedCategory == category) Color.White else WibukuMuted,
                                        fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp,
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
            val visibleSearchResults = if (!useMyNimeku || selectedCategory == "Semua") {
                searchResults
            } else {
                searchResults.filter { it.category == selectedCategory }
            }
            if (searchMode) {
                if (isSearching) {
                    PosterSkeletonGrid(Modifier.fillMaxSize())
                } else if (visibleSearchResults.isNotEmpty()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(visibleSearchResults) { anime ->
                            AnimeCardView(anime = anime) {
                                onAnimeClick(anime.slug, anime.title, anime.thumb)
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Tidak ada hasil untuk '$searchQuery'", color = WibukuMuted)
                    }
                }
            } else {
                val list = if (selectedTab == 0) ongoingList else completeList
                val visibleList = if (!useMyNimeku || selectedCategory == "Semua") {
                    list
                } else {
                    list.filter { it.category == selectedCategory }
                }

                if (isLoading) {
                    LoadingView()
                } else if (errorMessage != null && list.isEmpty()) {
                    ErrorView(message = errorMessage ?: "Terjadi kesalahan", onRetry = { loadData(initial = true) })
                } else if (visibleList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Belum ada $selectedCategory pada halaman ini.", color = WibukuMuted)
                    }
                } else {
                    val gridState = rememberLazyGridState()

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = gridState,
                        contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (selectedTab == 0 && !useMyNimeku) {
                            item(span = { GridItemSpan(2) }) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        StatCard("Riwayat", history.size.toString(), WibukuTeal, (history.size / 20f).coerceIn(.12f, 1f), Modifier.weight(1f))
                                        StatCard("Ongoing", ongoingList.size.toString(), WibukuPrimary, (ongoingList.size / 24f).coerceIn(.12f, 1f), Modifier.weight(1f))
                                        StatCard("Selesai", completeList.size.toString(), WibukuAccent, (completeList.size / 24f).coerceIn(.12f, 1f), Modifier.weight(1f))
                                    }
                                    HeroCarousel(visibleList.take(5)) { onAnimeClick(it.slug, it.title, it.thumb) }
                                    SectionHeader("Baru Rilis")
                                    PosterRail(visibleList) { onAnimeClick(it.slug, it.title, it.thumb) }
                                    history.firstOrNull()?.let { last ->
                                        SectionHeader("Lanjut Nonton", "History")
                                        ContinueWatchingCard(last.animeTitle, last.lastEpTitle, last.animeThumb, last.progress) {
                                            onAnimeClick(last.animeSlug, last.animeTitle, last.animeThumb)
                                        }
                                    }
                                    SectionHeader("Sedang Tayang", "${visibleList.size} judul")
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                                    ) {
                                        PulseDot(WibukuPrimary)
                                        Text("LIVE", color = WibukuPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                                        Text("Episode baru minggu ini", color = WibukuMuted, fontSize = 10.sp)
                                    }
                                    if (history.isNotEmpty()) {
                                        SectionHeader("Aktivitas Terbaru")
                                        history.take(2).forEach { recent ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)
                                                    .clip(RoundedCornerShape(14.dp)).background(WibukuSurface)
                                                    .clickable { onAnimeClick(recent.animeSlug, recent.animeTitle, recent.animeThumb) }
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(Modifier.size(38.dp, 48.dp).clip(RoundedCornerShape(8.dp))) {
                                                    PosterImage(recent.animeThumb, recent.animeTitle, Modifier.fillMaxSize())
                                                }
                                                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                                    Text(recent.animeTitle, color = WibukuText, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                                    Text("${recent.lastEpTitle} · aktivitas terbaru", color = WibukuMuted, fontSize = 10.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                                }
                                                ProgressTicks(recent.progress, Modifier.width(46.dp), WibukuTeal)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        itemsIndexed(visibleList, key = { _, anime -> anime.slug }) { index, anime ->
                            StaggeredItem(index = index) {
                                AnimeCardView(anime = anime) {
                                    onAnimeClick(anime.slug, anime.title, anime.thumb)
                                }
                            }
                        }

                        if (isLoadingMore) {
                            item(span = { GridItemSpan(2) }) {
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

                    LaunchedEffect(shouldLoadMore.value, isLoadingMore, isLoading) {
                        if (!shouldLoadMore.value) requestedPageAtEnd = false
                        if (shouldLoadMore.value && !requestedPageAtEnd && !isLoadingMore && !isLoading) {
                            requestedPageAtEnd = true
                            loadData(initial = false)
                        }
                    }
                }
            }
        }
    }
}
