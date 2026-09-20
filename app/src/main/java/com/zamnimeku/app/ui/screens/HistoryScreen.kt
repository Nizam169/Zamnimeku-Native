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
import com.zamnimeku.app.data.model.HistoryItem
import com.zamnimeku.app.data.storage.AppPreferences
import com.zamnimeku.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onHistoryClick: (String, String, String, Int) -> Unit
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
                        text = "Riwayat Menonton",
                        style = MaterialTheme.typography.titleLarge,
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
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(historyList) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onHistoryClick(item.animeSlug, item.animeTitle, item.animeThumb, item.lastEpIndex)
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = WibukuSurface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = item.animeThumb,
                                    contentDescription = item.animeTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(70.dp, 90.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.animeTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.lastEpTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = WibukuPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { item.progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = WibukuPrimary,
                                        trackColor = WibukuBorder
                                    )
                                }

                                IconButton(onClick = {
                                    prefs.deleteHistoryItem(item.animeSlug)
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
