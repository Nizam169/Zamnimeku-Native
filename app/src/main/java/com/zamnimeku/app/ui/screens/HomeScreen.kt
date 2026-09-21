package com.zamnimeku.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zamnimeku.app.data.api.OtakuApi
import com.zamnimeku.app.data.model.AnimeCard
import com.zamnimeku.app.ui.components.AnimeCardView
import com.zamnimeku.app.ui.components.ErrorView
import com.zamnimeku.app.ui.components.LoadingView
import com.zamnimeku.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAnimeClick: (String, String, String) -> Unit,
    onSearchClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    var ongoingList by remember { mutableStateOf<List<AnimeCard>>(emptyList()) }
    var completeList by remember { mutableStateOf<List<AnimeCard>>(emptyList()) }
    var ongoingPage by remember { mutableStateOf(1) }
    var completePage by remember { mutableStateOf(1) }

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<AnimeCard>>(emptyList()) }
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
                val results = OtakuApi.searchAnime(query.trim())
                searchResults = results
            } catch (_: Exception) {}
            finally {
                isSearching = false
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
                    val items = OtakuApi.getOngoingAnime(p)
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
                    val items = OtakuApi.getCompleteAnime(p)
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

    LaunchedEffect(selectedTab) {
        if (selectedTab == 0 && ongoingList.isEmpty()) {
            loadData(initial = true)
        } else if (selectedTab == 1 && completeList.isEmpty()) {
            loadData(initial = true)
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
                    // ── WIBUKU HEADER ──
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
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ZAMNIMEKU",
                                style = MaterialTheme.typography.titleLarge,
                                color = WibukuText,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp,
                                fontSize = 20.sp
                            )
                            Text(
                                text = "Nonton anime sub Indo",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WibukuMuted,
                                fontSize = 12.sp
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
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            color = Color(0xFFE3F2FD),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp)
                            ) {
                                val tabs = listOf("Ongoing Anime", "Complete Anime")
                                tabs.forEachIndexed { index, title ->
                                    val isSelected = selectedTab == index
                                    val tabBg by animateColorAsState(
                                        targetValue = if (isSelected) WibukuPrimary else Color.Transparent,
                                        animationSpec = tween(durationMillis = 200),
                                        label = "tabBg"
                                    )
                                    val tabColor by animateColorAsState(
                                        targetValue = if (isSelected) Color.White else WibukuMuted,
                                        animationSpec = tween(durationMillis = 200),
                                        label = "tabColor"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(tabBg)
                                            .clickable { selectedTab = index }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = title,
                                            color = tabColor,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        )
                                    }
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
            if (searchMode) {
                if (isSearching) {
                    LoadingView(text = "Mencari anime '$searchQuery'...")
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
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Tidak ada hasil untuk '$searchQuery'", color = WibukuMuted)
                    }
                }
            } else {
                val list = if (selectedTab == 0) ongoingList else completeList

                if (isLoading) {
                    LoadingView()
                } else if (errorMessage != null && list.isEmpty()) {
                    ErrorView(message = errorMessage ?: "Terjadi kesalahan", onRetry = { loadData(initial = true) })
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
                        items(list) { anime ->
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
                        if (shouldLoadMore.value && !isLoadingMore && !isLoading) {
                            loadData(initial = false)
                        }
                    }
                }
            }
        }
    }
}
