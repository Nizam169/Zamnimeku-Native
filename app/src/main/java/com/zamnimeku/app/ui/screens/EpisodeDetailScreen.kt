package com.zamnimeku.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zamnimeku.app.data.api.OtakuApi
import com.zamnimeku.app.data.model.AnimeDetail
import com.zamnimeku.app.data.storage.AppPreferences
import com.zamnimeku.app.ui.components.ErrorView
import com.zamnimeku.app.ui.components.LoadingView
import com.zamnimeku.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeDetailScreen(
    animeSlug: String,
    animeTitle: String,
    animeThumb: String,
    onBack: () -> Unit,
    onEpisodeClick: (Int, String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    var detail by remember { mutableStateOf<AnimeDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val history = remember { prefs.getHistory() }
    val lastWatched = history.firstOrNull { it.animeSlug == animeSlug }

    LaunchedEffect(animeSlug) {
        isLoading = true
        errorMessage = null
        try {
            val d = OtakuApi.getAnimeDetail(animeSlug)
            detail = d
            if (d == null) errorMessage = "Gagal memuat detail anime."
        } catch (e: Exception) {
            errorMessage = "Terjadi kesalahan: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = detail?.title ?: animeTitle,
                        maxLines = 1,
                        overflow = TextOverflow.ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WibukuSurface)
            )
        },
        containerColor = WibukuBg
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                LoadingView(text = "Memuat episode & informasi...")
            } else if (errorMessage != null || detail == null) {
                ErrorView(message = errorMessage ?: "Data tidak ditemukan", onRetry = { /* reload */ })
            } else {
                val d = detail!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Header / Poster & Info
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = WibukuSurface,
                            shadowElevation = 1.dp
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    AsyncImage(
                                        model = d.thumb.ifEmpty { animeThumb },
                                        contentDescription = d.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .width(110.dp)
                                            .aspectRatio(0.72f)
                                            .clip(RoundedCornerShape(10.dp))
                                    )

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = d.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = WibukuText
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = d.score, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = WibukuText)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Surface(
                                                color = WibukuBadge,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = d.status,
                                                    color = WibukuPrimary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "Studio: ${d.studio}", style = MaterialTheme.typography.bodyMedium)
                                        Text(text = "Total Ep: ${d.totalEpisodes} (${d.duration})", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }

                                // Genre chips
                                if (d.genres.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(d.genres) { g ->
                                            Surface(
                                                color = WibukuBg,
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Text(
                                                    text = g,
                                                    color = WibukuMuted,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Synopsis
                                if (d.synopsis.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Sinopsis",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = d.synopsis,
                                        style = MaterialTheme.typography.bodyMedium,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }

                    // Episodes Header
                    item {
                        PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Daftar Episode (${d.episodes.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Episode List
                    itemsIndexed(d.episodes) { index, ep ->
                        val isWatched = lastWatched?.lastEpSlug == ep.slug
                        val progress = if (isWatched) lastWatched?.progress ?: 0f else 0f

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onEpisodeClick(index, ep.slug)
                                },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isWatched) WibukuBadge else WibukuSurface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (progress >= 0.9f) Icons.Rounded.CheckCircle else Icons.Rounded.PlayCircle,
                                        contentDescription = null,
                                        tint = if (isWatched) WibukuPrimary else WibukuMuted,
                                        modifier = Modifier.size(26.dp)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ep.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (isWatched) WibukuPrimary else WibukuText,
                                            maxLines = 1,
                                            overflow = TextOverflow.ellipsis
                                        )
                                        if (ep.date.isNotEmpty()) {
                                            Text(
                                                text = ep.date,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                if (progress > 0f) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = WibukuPrimary,
                                        trackColor = WibukuBorder
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
