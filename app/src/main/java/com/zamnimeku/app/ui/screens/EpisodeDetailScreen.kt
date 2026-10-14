package com.zamnimeku.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.PlayCircleFilled
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zamnimeku.app.data.api.AnimeApi
import com.zamnimeku.app.data.api.OtakuApi
import com.zamnimeku.app.data.model.AnimeDetail
import com.zamnimeku.app.data.model.AnimeSource
import com.zamnimeku.app.data.storage.AppPreferences
import com.zamnimeku.app.ui.components.ErrorView
import com.zamnimeku.app.ui.components.LoadingView
import com.zamnimeku.app.ui.theme.*
import com.zamnimeku.app.ui.components.PosterImage
import com.zamnimeku.app.ui.components.ProgressTicks
import com.zamnimeku.app.ui.components.StaggeredItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeDetailScreen(
    animeSlug: String,
    animeTitle: String,
    animeThumb: String,
    source: AnimeSource = AnimeSource.OTAKUDESU,
    onBack: () -> Unit,
    onEpisodeClick: (Int, String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<AnimeDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableStateOf(0) }
    var synopsisExpanded by remember { mutableStateOf(false) }
    var inWatchlist by remember(animeSlug, source) { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val history = remember { prefs.getHistory() }
    val lastWatched = history.firstOrNull { it.animeSlug == animeSlug && it.source == source }

    LaunchedEffect(animeSlug, source, retryTrigger) {
        isLoading = true
        errorMessage = null
        try {
                val d = if (source == AnimeSource.MYNIMEKU) {
                    AnimeApi.getAnimeDetail(animeSlug)
                } else {
                    OtakuApi.getAnimeDetail(animeSlug)
                }
            detail = d
            if (d == null) errorMessage = "Gagal memuat detail anime."
        } catch (e: Exception) {
            errorMessage = "Terjadi kesalahan: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                ErrorView(message = errorMessage ?: "Data tidak ditemukan", onRetry = { retryTrigger++ })
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
                            Column {
                                Box(Modifier.fillMaxWidth().height(280.dp)) {
                                    PosterImage(d.thumb.ifEmpty { animeThumb }, d.title, Modifier.fillMaxSize())
                                    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, WibukuSurface))))
                                    Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                                        Text("ANIME · ${d.status.uppercase()}", color = WibukuAccent, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                                        Text(d.title, style = MaterialTheme.typography.headlineLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                    IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(start = 10.dp, top = 4.dp)) {
                                        Icon(Icons.Rounded.ArrowBack, "Kembali", tint = WibukuText)
                                    }
                                }
                                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Icon(Icons.Rounded.Star, contentDescription = null, tint = WibukuAccent, modifier = Modifier.size(18.dp))
                                        Text(d.score, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = WibukuText)
                                        Text("· ${d.releaseDate}", color = WibukuMuted, fontSize = 12.sp)
                                        Text("· ${d.totalEpisodes} episode", color = WibukuMuted, fontSize = 12.sp)
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = {
                                            if (d.episodes.isNotEmpty()) {
                                                val startIndex = lastWatched?.lastEpSlug?.let { slug -> d.episodes.indexOfFirst { it.slug == slug } }
                                                    ?.takeIf { it >= 0 } ?: 0
                                                onEpisodeClick(startIndex, d.episodes[startIndex].slug)
                                            }
                                        }, colors = ButtonDefaults.buttonColors(containerColor = WibukuPrimary)) {
                                            Icon(Icons.Rounded.PlayArrow, null)
                                            Spacer(Modifier.width(6.dp))
                                            Text("Tonton")
                                        }
                                        OutlinedButton(onClick = {
                                            inWatchlist = !inWatchlist
                                            scope.launch { snackbarHostState.showSnackbar(if (inWatchlist) "Ditambahkan ke watchlist" else "Dihapus dari watchlist") }
                                        }, border = androidx.compose.foundation.BorderStroke(1.dp, WibukuBorder)) {
                                            Icon(if (inWatchlist) Icons.Rounded.Check else Icons.Rounded.BookmarkAdd, null)
                                            Spacer(Modifier.width(5.dp))
                                            Text(if (inWatchlist) "Di Watchlist" else "Watchlist")
                                        }
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                        Text("Studio ${d.studio}", color = WibukuMuted, fontSize = 11.sp)
                                        Text("·", color = WibukuMuted)
                                        Text(d.type, color = WibukuMuted, fontSize = 11.sp)
                                    }
                                }
                                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Sinopsis", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                        Text(if (synopsisExpanded) "Lebih sedikit" else "Baca selengkapnya", color = WibukuPrimary, fontSize = 11.sp, modifier = Modifier.clickable { synopsisExpanded = !synopsisExpanded })
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(d.synopsis, style = MaterialTheme.typography.bodyMedium, maxLines = if (synopsisExpanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis, lineHeight = 19.sp)
                                }
                                if (d.genres.isNotEmpty()) {
                                    LazyRow(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                        items(d.genres) { genre ->
                                            Surface(color = WibukuCard, shape = RoundedCornerShape(16.dp)) {
                                                Text(genre, color = WibukuMuted, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                                            }
                                        }
                                    }
                                }

                        }
                    }

                    // Episodes Header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("LANJUTKAN CERITA", color = WibukuAccent, fontSize = 9.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Black)
                                Text(text = "Daftar Episode · ${d.episodes.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // ── DAFTAR EPISODE DENGAN TIMELINE WATCH PROGRESS ──
                    itemsIndexed(d.episodes) { index, ep ->
                        val epProg = prefs.getEpisodeProgress(ep.slug, source)
                        val hasWatched = epProg > 0f
                        val isDone = epProg >= 0.9f
                        val isLastWatched = lastWatched?.lastEpSlug == ep.slug

                        StaggeredItem(index) { Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onEpisodeClick(index, ep.slug)
                                },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isLastWatched) WibukuBadge else WibukuSurface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isDone) Icons.Rounded.CheckCircle else if (hasWatched) Icons.Rounded.PlayCircleFilled else Icons.Rounded.PlayCircle,
                                        contentDescription = null,
                                        tint = if (isDone) WibukuTeal else if (hasWatched) WibukuPrimary else WibukuMuted,
                                        modifier = Modifier.size(26.dp)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ep.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (isLastWatched) WibukuPrimary else WibukuText,
                                            textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                                            fontWeight = if (isLastWatched || hasWatched) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
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

                                // ── PROGRESS TIMELINE BAR ALA FLUTTER ──
                                if (hasWatched) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    ProgressTicks(epProg, Modifier.fillMaxWidth(), if (isDone) WibukuTeal else WibukuPrimary)
                                }
                            }
                        } }
                    }
                }
            }
        }
    }
}
