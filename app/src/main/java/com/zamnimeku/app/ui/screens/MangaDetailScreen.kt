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
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zamnimeku.app.data.api.MangaApi
import com.zamnimeku.app.data.model.MangaChapter
import com.zamnimeku.app.data.model.MangaDetail
import com.zamnimeku.app.ui.components.ErrorView
import com.zamnimeku.app.ui.components.LoadingView
import com.zamnimeku.app.ui.theme.*
import com.zamnimeku.app.ui.components.PosterImage
import com.zamnimeku.app.ui.components.StaggeredItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaDetailScreen(
    mangaSlug: String,
    mangaTitle: String,
    mangaThumb: String,
    onBack: () -> Unit,
    onChapterClick: (String, String) -> Unit
) {
    var detail by remember { mutableStateOf<MangaDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isAscending by remember { mutableStateOf(false) }
    var retryTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(mangaSlug, retryTrigger) {
        isLoading = true
        errorMessage = null
        try {
            val d = MangaApi.getMangaDetail(mangaSlug)
            detail = d
            if (d == null) errorMessage = "Gagal memuat informasi komik."
        } catch (e: Exception) {
            errorMessage = "Terjadi kesalahan: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
                TopAppBar(
                    title = { Text(detail?.title ?: mangaTitle, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Kembali") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = WibukuBg)
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
                LoadingView(text = "Memuat detail komik & chapter...")
            } else if (errorMessage != null || detail == null) {
                ErrorView(message = errorMessage ?: "Data komik tidak ditemukan", onRetry = { retryTrigger++ })
            } else {
                val d = detail!!
                val displayChapters = if (isAscending) d.chapters.reversed() else d.chapters

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Header Card
                    item {
                        Surface(modifier = Modifier.fillMaxWidth(), color = WibukuBg) {
                            Column {
                                Box(Modifier.fillMaxWidth().height(250.dp)) {
                                    PosterImage(d.thumb.ifEmpty { mangaThumb }, d.title, Modifier.fillMaxSize())
                                    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, WibukuBg))))
                                    Text(d.title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.align(Alignment.BottomStart).padding(18.dp))
                                }
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(color = WibukuBadge, shape = RoundedCornerShape(7.dp)) {
                                            Text(d.status, color = WibukuPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                                        }
                                        Surface(color = WibukuCard, shape = RoundedCornerShape(7.dp)) {
                                            Text(d.type, color = WibukuAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text("${d.author} · ${d.chapters.size} chapter", color = WibukuMuted, style = MaterialTheme.typography.bodyMedium)

                                // Genres
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
                    }

                    // Chapter List Header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Daftar Chapter (${d.chapters.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { isAscending = !isAscending },
                                color = WibukuBadge,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.SwapVert, contentDescription = null, tint = WibukuPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isAscending) "Terlama" else "Terbaru",
                                        color = WibukuPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Chapters
                    itemsIndexed(displayChapters, key = { _, chapter -> chapter.slug }) { index, ch ->
                        StaggeredItem(index) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onChapterClick(ch.slug, ch.title) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = WibukuSurface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(WibukuBadge),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.AutoStories, contentDescription = null, tint = WibukuPrimary, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ch.title, style = MaterialTheme.typography.titleSmall, color = WibukuText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (ch.date.isNotEmpty()) Text(ch.date, style = MaterialTheme.typography.bodyMedium, fontSize = 11.sp)
                                    }
                                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = WibukuMuted)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
