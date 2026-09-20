package com.zamnimeku.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zamnimeku.app.data.api.OtakuApi
import com.zamnimeku.app.data.model.ScheduleDay
import com.zamnimeku.app.ui.components.ErrorView
import com.zamnimeku.app.ui.components.LoadingView
import com.zamnimeku.app.ui.theme.*

@Composable
fun ScheduleScreen(
    onAnimeClick: (String, String, String) -> Unit
) {
    var scheduleList by remember { mutableStateOf<List<ScheduleDay>>(emptyList()) }
    var selectedDayIndex by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadSchedule() {
        isLoading = true
        errorMessage = null
        kotlinx.coroutines.GlobalScope.let {
            // will be launched in LaunchedEffect
        }
    }

    LaunchedEffect(Unit) {
        try {
            isLoading = true
            val list = OtakuApi.getSchedule()
            scheduleList = list
        } catch (e: Exception) {
            errorMessage = "Gagal memuat jadwal rilis: ${e.message}"
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
                        text = "Jadwal Rilis Anime",
                        style = MaterialTheme.typography.titleLarge,
                        color = WibukuText,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
                    )

                    if (scheduleList.isNotEmpty()) {
                        ScrollableTabRow(
                            selectedTabIndex = selectedDayIndex,
                            containerColor = WibukuSurface,
                            contentColor = WibukuPrimary,
                            edgePadding = 12.dp,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedDayIndex]),
                                    color = WibukuPrimary,
                                    height = 3.dp
                                )
                            }
                        ) {
                            scheduleList.forEachIndexed { index, day ->
                                Tab(
                                    selected = selectedDayIndex == index,
                                    onClick = { selectedDayIndex = index },
                                    text = {
                                        Text(
                                            day.day,
                                            fontWeight = if (selectedDayIndex == index) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                )
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
            if (isLoading) {
                LoadingView(text = "Memuat jadwal tayang...")
            } else if (errorMessage != null) {
                ErrorView(message = errorMessage!, onRetry = { /* reload */ })
            } else if (scheduleList.isNotEmpty() && selectedDayIndex in scheduleList.indices) {
                val currentDay = scheduleList[selectedDayIndex]
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(currentDay.animes) { anime ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onAnimeClick(anime.slug, anime.title, anime.thumb)
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = WibukuSurface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(WibukuBadge),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.PlayCircle,
                                        contentDescription = null,
                                        tint = WibukuPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = anime.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                    overflow = TextOverflow.ellipsis
                                )

                                Icon(
                                    Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = WibukuMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
