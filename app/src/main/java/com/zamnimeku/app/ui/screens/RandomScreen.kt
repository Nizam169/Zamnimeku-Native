package com.zamnimeku.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zamnimeku.app.data.api.OtakuApi
import com.zamnimeku.app.data.model.AnimeCard
import com.zamnimeku.app.ui.components.LoadingView
import com.zamnimeku.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun RandomScreen(
    onAnimeClick: (String, String, String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var pool by remember { mutableStateOf<List<AnimeCard>>(emptyList()) }
    var currentAnime by remember { mutableStateOf<AnimeCard?>(null) }
    var isRolling by remember { mutableStateOf(false) }
    var isLoadingPool by remember { mutableStateOf(true) }

    suspend fun fetchPool() {
        isLoadingPool = true
        try {
            val p1 = OtakuApi.getOngoingAnime(1)
            val p2 = OtakuApi.getCompleteAnime(1)
            val combined = (p1 + p2).shuffled()
            pool = combined
            if (combined.isNotEmpty()) {
                currentAnime = combined.first()
            }
        } catch (_: Exception) {}
        finally {
            isLoadingPool = false
        }
    }

    LaunchedEffect(Unit) {
        fetchPool()
    }

    fun rollAnime() {
        if (pool.isEmpty() || isRolling) return
        scope.launch {
            isRolling = true
            for (i in 0 until 12) {
                currentAnime = pool[Random.nextInt(pool.size)]
                delay(60L + i * 15L)
            }
            isRolling = false
        }
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Temukan Anime Acak",
                        style = MaterialTheme.typography.titleLarge,
                        color = WibukuText
                    )
                }
            }
        },
        containerColor = WibukuBg
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (isLoadingPool) {
                LoadingView(text = "Menyiapkan koleksi anime...")
            } else if (currentAnime != null) {
                val anime = currentAnime!!
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.82f)
                            .aspectRatio(0.72f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                onAnimeClick(anime.slug, anime.title, anime.thumb)
                            },
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = anime.thumb,
                                contentDescription = anime.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.withOpacity(0.75f)),
                                            startY = 200f
                                        )
                                    )
                            )

                            if (anime.episode.isNotEmpty()) {
                                Surface(
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .align(Alignment.TopStart),
                                    color = WibukuPrimary,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = anime.episode,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = anime.title,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.ellipsis,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(0.85f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { rollAnime() },
                            enabled = !isRolling,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WibukuPrimary)
                        ) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Acak Lagi", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onAnimeClick(anime.slug, anime.title, anime.thumb) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WibukuPrimary)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Nonton", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
