package com.zamnimeku.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.zamnimeku.app.ui.theme.WibukuAccent
import com.zamnimeku.app.ui.theme.WibukuBadge
import com.zamnimeku.app.ui.theme.WibukuBg
import com.zamnimeku.app.ui.theme.WibukuBorder
import com.zamnimeku.app.ui.theme.WibukuCard
import com.zamnimeku.app.ui.theme.WibukuMuted
import com.zamnimeku.app.ui.theme.WibukuPrimary
import com.zamnimeku.app.ui.theme.WibukuSurface
import com.zamnimeku.app.ui.theme.WibukuTeal
import com.zamnimeku.app.ui.theme.WibukuText
import com.zamnimeku.app.data.model.AnimeCard
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items

@Composable
fun PosterImage(
    url: String,
    title: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(),
        contentDescription = title,
        contentScale = contentScale,
        modifier = modifier,
        loading = { PosterPlaceholder(title) },
        error = { PosterPlaceholder(title, isError = true) }
    )
}

@Composable
fun PosterPlaceholder(title: String, isError: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WibukuBadge),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isError) WibukuPrimary.copy(alpha = .25f) else WibukuTeal.copy(alpha = .2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isError) Icons.Rounded.BrokenImage else Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = if (isError) WibukuPrimary else WibukuTeal,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = title.take(14),
            color = WibukuText.copy(alpha = .7f),
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomCenter).padding(6.dp)
        )
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (action != null) {
            Text(
                action,
                color = WibukuPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onAction?.invoke() }
                )
            )
        }
    }
}

@Composable
fun SegmentedTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier, color = WibukuSurface, shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.padding(4.dp)) {
            labels.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val color by animateColorAsState(if (selected) WibukuPrimary else Color.Transparent, tween(220), label = "segmentColor")
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(color)
                        .clickable { onSelected(index) }.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = if (selected) WibukuText else WibukuMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, accent: Color, progress: Float, modifier: Modifier = Modifier) {
    val animatedProgress by animateFloatAsState(progress.coerceIn(0f, 1f), tween(850, easing = FastOutSlowInEasing), label = "statProgress")
    Card(modifier = modifier, colors = CardDefaults.cardColors(WibukuCard), shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = accent, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(3.dp))
            Text(label, color = WibukuMuted, fontSize = 11.sp, maxLines = 1)
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(4.dp)),
                color = accent,
                trackColor = WibukuBorder
            )
        }
    }
}

@Composable
fun ProgressTicks(progress: Float, modifier: Modifier = Modifier, color: Color = WibukuPrimary) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(900), label = "progressTicks")
    LinearProgressIndicator(
        progress = { animated },
        modifier = modifier.height(4.dp).clip(RoundedCornerShape(4.dp)),
        color = color,
        trackColor = WibukuBorder
    )
}

@Composable
fun PulseDot(color: Color = WibukuPrimary) {
    val transition = rememberInfiniteTransition(label = "pulseDot")
    val alpha by transition.animateFloat(
        initialValue = .35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
        label = "pulseAlpha"
    )
    Box(Modifier.size(8.dp).clip(CircleShape).background(color.copy(alpha = alpha)))
}

@Composable
fun StaggeredItem(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index.coerceAtMost(8) * 45L))
        visible = true
    }
    AnimatedVisibility(visible, enter = fadeIn(tween(380)) + slideInVertically(tween(380)) { it / 4 }) { content() }
}

@Composable
fun ContinueWatchingCard(title: String, episode: String, thumb: String, progress: Float, onClick: () -> Unit) {
    val animatedProgress by animateFloatAsState(progress.coerceIn(0f, 1f), tween(900), label = "continueProgress")
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(WibukuSurface),
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(112.dp, 72.dp).clip(RoundedCornerShape(14.dp))) { PosterImage(thumb, title) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(episode, color = WibukuPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                ProgressTicks(animatedProgress, Modifier.fillMaxWidth(), WibukuTeal)
            }
        }
    }
}

@Composable
fun HeroCarousel(items: List<AnimeCard>, onAnimeClick: (AnimeCard) -> Unit) {
    var page by remember(items) { mutableStateOf(0) }
    LaunchedEffect(items) {
        if (items.size > 1) while (true) {
            kotlinx.coroutines.delay(4200)
            page = (page + 1) % items.size
        }
    }
    val anime = items.getOrNull(page) ?: return
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { onAnimeClick(anime) },
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = WibukuSurface)
    ) {
        Box(Modifier.fillMaxWidth().height(222.dp)) {
            PosterImage(anime.thumb, anime.title, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, WibukuBg.copy(alpha = .96f)))))
            Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
                Text("RILIS HARI INI", color = WibukuAccent, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(5.dp))
                Text(anime.title, style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(anime.episode.ifBlank { anime.day.ifBlank { "Temukan episode terbaru" } }, color = WibukuMuted, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    items.forEachIndexed { index, _ ->
                        val width by animateFloatAsState(if (index == page) 22f else 5f, tween(250), label = "heroIndicator")
                        Box(Modifier.width(width.dp).height(5.dp).clip(CircleShape).background(if (index == page) WibukuPrimary else WibukuText.copy(alpha = .35f)))
                    }
                }
            }
        }
    }
}

@Composable
fun PosterRail(items: List<AnimeCard>, onAnimeClick: (AnimeCard) -> Unit) {
    LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(items.take(10), key = { item -> item.slug }) { anime ->
            Column(
                modifier = Modifier.width(116.dp).clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null
                ) { onAnimeClick(anime) }
            ) {
                Box(Modifier.fillMaxWidth().aspectRatio(.72f).clip(RoundedCornerShape(16.dp))) {
                    PosterImage(anime.thumb, anime.title, Modifier.fillMaxSize())
                    if (anime.episode.isNotBlank()) {
                        Surface(Modifier.align(Alignment.TopStart).padding(7.dp), color = WibukuPrimary, shape = RoundedCornerShape(7.dp)) {
                            Text(anime.episode, color = WibukuText, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp), maxLines = 1)
                        }
                    }
                }
                Spacer(Modifier.height(7.dp))
                Text(anime.title, color = WibukuText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun PosterSkeletonGrid(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "posterSkeleton")
    val shift by transition.animateFloat(-.8f, 1.8f, infiniteRepeatable(tween(1000), RepeatMode.Restart), label = "posterShimmer")
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(6) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(.72f).clip(RoundedCornerShape(16.dp))
                        .background(Brush.horizontalGradient(listOf(WibukuSurface, WibukuBorder, WibukuSurface), startX = shift * 600f, endX = shift * 600f + 600f))
                )
                Box(Modifier.fillMaxWidth(.84f).height(11.dp).clip(RoundedCornerShape(8.dp)).background(WibukuCard))
                Box(Modifier.fillMaxWidth(.52f).height(9.dp).clip(RoundedCornerShape(8.dp)).background(WibukuSurface))
            }
        }
    }
}

@Preview
@Composable
private fun PremiumComponentsPreview() {
    Surface(color = WibukuBg) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("Lanjut Nonton", "Lihat semua")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Ditonton", "24", WibukuTeal, .7f, Modifier.weight(1f))
                StatCard("Watchlist", "8", WibukuAccent, .35f, Modifier.weight(1f))
            }
            ContinueWatchingCard("Eclipse Academy", "Episode 07", "", .58f) {}
        }
    }
}
