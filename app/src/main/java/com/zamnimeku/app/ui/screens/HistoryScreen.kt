package com.zamnimeku.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zamnimeku.app.data.model.AnimeSource
import com.zamnimeku.app.data.model.HistoryItem
import com.zamnimeku.app.data.storage.AppPreferences
import com.zamnimeku.app.ui.theme.*
import com.zamnimeku.app.ui.components.PosterImage
import com.zamnimeku.app.ui.components.ProgressTicks
import com.zamnimeku.app.ui.components.SectionHeader
import com.zamnimeku.app.ui.components.StatCard
import com.zamnimeku.app.ui.components.StaggeredItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onHistoryClick: (String, String, String, Int, AnimeSource) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    var historyList by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }

    fun refresh() {
        historyList = prefs.getHistory()
    }

    LaunchedEffect(Unit) {
        refresh()
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Koleksi",
                        style = MaterialTheme.typography.headlineMedium,
                        color = WibukuText
                    )

                    if (historyList.isNotEmpty()) {
                        IconButton(onClick = {
                            prefs.clearAllHistory()
                            refresh()
                        }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Hapus Riwayat", tint = WibukuMuted)
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
            if (historyList.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = WibukuMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Belum ada riwayat tontonan",
                        style = MaterialTheme.typography.titleMedium,
                        color = WibukuMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Anime yang kamu tonton akan muncul di sini",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatCard("Ditonton", historyList.size.toString(), WibukuTeal, (historyList.size / 20f).coerceIn(.1f, 1f), Modifier.weight(1f))
                            StatCard("Sedang Jalan", historyList.count { it.progress in 0.02f..0.98f }.toString(), WibukuPrimary, .55f, Modifier.weight(1f))
                            StatCard("Selesai", historyList.count { it.progress >= .98f }.toString(), WibukuAccent, .3f, Modifier.weight(1f))
                        }
                    }
                    item { SectionHeader("Terakhir Ditonton", "${historyList.size} judul") }
                    items(historyList, key = { "${it.source.name}:${it.animeSlug}" }) { item ->
                        StaggeredItem(historyList.indexOf(item)) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    onHistoryClick(item.animeSlug, item.animeTitle, item.animeThumb, item.lastEpIndex, item.source)
                                },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = WibukuSurface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(76.dp, 98.dp).clip(RoundedCornerShape(12.dp))) {
                                    PosterImage(item.animeThumb, item.animeTitle, Modifier.fillMaxSize())
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.animeTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.lastEpTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = WibukuPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    ProgressTicks(item.progress, Modifier.fillMaxWidth(), WibukuPrimary)
                                }

                                IconButton(onClick = {
                                    prefs.deleteHistoryItem(item.animeSlug, item.source)
                                    refresh()
                                }) {
                                    Icon(
                                        Icons.Rounded.DeleteOutline,
                                        contentDescription = "Hapus",
                                        tint = WibukuMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        }
                    }
                }
            }
        }
    }
}
