package com.zamnimeku.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                color = WibukuSurface,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Zamnimeku",
                                style = MaterialTheme.typography.titleLarge,
                                color = WibukuPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = WibukuBadge,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Sub Indo",
                                    color = WibukuPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        IconButton(onClick = onSearchClick) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Cari Anime",
                                tint = WibukuText
                            )
                        }
                    }

                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = WibukuSurface,
                        contentColor = WibukuPrimary,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = WibukuPrimary,
                                height = 3.dp
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Text(
                                    "Ongoing Anime",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Text(
                                    "Complete Anime",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }
            }
        },
        containerColor = WibukuBg
    ) { padding ->
        val list = if (selectedTab == 0) ongoingList else completeList

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                LoadingView()
            } else if (errorMessage != null && list.isEmpty()) {
                ErrorView(message = errorMessage!!, onRetry = { loadData(initial = true) })
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
