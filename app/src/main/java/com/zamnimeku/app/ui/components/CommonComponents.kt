package com.zamnimeku.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zamnimeku.app.data.model.AnimeCard
import com.zamnimeku.app.data.model.MangaCard
import com.zamnimeku.app.ui.theme.*

enum class NavTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Rounded.Home),
    GENRE("Genre", Icons.Rounded.Category),
    RANDOM("Random", Icons.Rounded.Shuffle),
    SCHEDULE("Jadwal", Icons.Rounded.CalendarMonth),
    MANGA("Komik", Icons.Rounded.AutoStories),
    HISTORY("Riwayat", Icons.Rounded.History)
}

@Composable
fun AppBottomBar(
    currentTab: NavTab,
    onTabSelected: (NavTab) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = WibukuSurface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTab.values().forEach { tab ->
                val isSelected = tab == currentTab
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) WibukuBadge else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = if (isSelected) WibukuPrimary else WibukuNavInactive,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) WibukuPrimary else WibukuNavInactive,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AnimeCardView(
    anime: AnimeCard,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WibukuSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
            ) {
                AsyncImage(
                    model = anime.thumb,
                    contentDescription = anime.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Gradient bottom
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.withOpacity(0.7f)),
                                startY = 150f
                            )
                        )
                )

                // Episode badge
                if (anime.episode.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .padding(6.dp)
                            .align(Alignment.TopStart),
                        color = WibukuPrimary.withOpacity(0.9f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = anime.episode,
                            color = Color.white,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Day / Score badge
                val tag = if (anime.score.isNotEmpty()) "★ ${anime.score}" else anime.day
                if (tag.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .padding(6.dp)
                            .align(Alignment.BottomEnd),
                        color = Color.Black.withOpacity(0.75f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = tag,
                            color = Color.white,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = anime.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.ellipsis,
                modifier = Modifier.padding(8.dp),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun MangaCardView(
    manga: MangaCard,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WibukuSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
            ) {
                AsyncImage(
                    model = manga.thumb,
                    contentDescription = manga.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Text(
                text = manga.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.ellipsis,
                modifier = Modifier.padding(8.dp),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun LoadingView(modifier: Modifier = Modifier, text: String = "Memuat data...") {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = WibukuPrimary, strokeWidth = 3.dp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.CloudOff,
                contentDescription = null,
                tint = WibukuMuted,
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = WibukuPrimary)
            ) {
                Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Coba Lagi", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun QualitySelectionDialog(
    currentQuality: String,
    onQualitySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // 4 Resolusi Lengkap sampai 1080p
    val qualities = listOf(
        Pair("360p", "Hemat Kuota • Paling Cepat"),
        Pair("480p", "Kualitas Standar (SD) • Lancar"),
        Pair("720p", "High Definition (HD) • Jernih"),
        Pair("1080p", "Ultra Full HD (FHD) • Sangat Jernih")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WibukuDark,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.HighQuality, contentDescription = null, tint = WibukuPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Kualitas Video", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                qualities.forEach { (q, desc) ->
                    val isSelected = q == currentQuality
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                onQualitySelected(q)
                                onDismiss()
                            },
                        color = if (isSelected) WibukuPrimary.withOpacity(0.2f) else Color.White.withOpacity(0.08)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = if (isSelected) WibukuPrimary else Color.White.withOpacity(0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = q,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = q, color = if (isSelected) WibukuPrimary else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = desc, color = Color.White.withOpacity(0.6f), fontSize = 10.sp)
                            }
                            if (isSelected) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = WibukuPrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}
