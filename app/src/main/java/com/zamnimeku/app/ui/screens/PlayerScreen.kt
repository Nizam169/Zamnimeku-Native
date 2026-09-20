package com.zamnimeku.app.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.zamnimeku.app.data.api.OtakuApi
import com.zamnimeku.app.data.model.Episode
import com.zamnimeku.app.data.model.HistoryItem
import com.zamnimeku.app.data.model.VideoSource
import com.zamnimeku.app.data.storage.AppPreferences
import com.zamnimeku.app.ui.components.QualitySelectionDialog
import com.zamnimeku.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    episodes: List<Episode>,
    initialIndex: Int,
    animeTitle: String,
    animeSlug: String,
    animeThumb: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val prefs = remember { AppPreferences(context) }
    val scope = rememberCoroutineScope()

    var currentEpIndex by remember { mutableStateOf(initialIndex) }
    val currentEp = episodes.getOrNull(currentEpIndex) ?: Episode(title = "Episode", slug = "", url = "")

    var isLandscape by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var showQualityDialog by remember { mutableStateOf(false) }

    var candidates by remember { mutableStateOf<List<VideoSource>>(emptyList()) }
    var selectedQuality by remember { mutableStateOf(prefs.preferredQuality) }
    var isLoading by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var currentPositionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    val speedList = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    // ExoPlayer instance
    val exoPlayer = remember {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)
            .setAllowCrossProtocolRedirects(true)

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build()
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    isLoading = false
                    durationMs = exoPlayer.duration.coerceAtLeast(0L)
                } else if (state == Player.STATE_ENDED) {
                    if (currentEpIndex > 0) {
                        currentEpIndex -= 1
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                isLoading = false
                errorMessage = "Gagal memutar video. Silakan ganti resolusi atau coba lagi."
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    // Save Progress Periodic
    LaunchedEffect(isPlaying, currentPositionMs) {
        while (isPlaying) {
            currentPositionMs = exoPlayer.currentPosition
            durationMs = exoPlayer.duration.coerceAtLeast(0L)
            if (durationMs > 0) {
                val prog = (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                prefs.saveHistory(
                    HistoryItem(
                        animeSlug = animeSlug,
                        animeTitle = animeTitle,
                        animeThumb = animeThumb,
                        lastEpSlug = currentEp.slug,
                        lastEpTitle = currentEp.title,
                        lastEpIndex = currentEpIndex,
                        positionMs = currentPositionMs,
                        durationMs = durationMs,
                        progress = prog,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            delay(3000)
        }
    }

    // Auto hide controls
    LaunchedEffect(showControls, isPlaying, isLocked) {
        if (showControls && isPlaying && !isLocked) {
            delay(4500)
            showControls = false
        }
    }

    fun playUrl(url: String) {
        isLoading = true
        errorMessage = null
        try {
            val mediaItem = MediaItem.fromUri(Uri.parse(url))
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } catch (e: Exception) {
            errorMessage = "Gagal memuat URL: ${e.message}"
            isLoading = false
        }
    }

    fun loadEpisodeVideo() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val list = OtakuApi.getVideoCandidates(currentEp.slug)
                candidates = list
                val target = list.firstOrNull { it.quality == selectedQuality } ?: list.firstOrNull()
                if (target != null) {
                    playUrl(target.url)
                } else {
                    errorMessage = "Video tidak ditemukan di server."
                    isLoading = false
                }
            } catch (e: Exception) {
                errorMessage = "Gagal memuat video: ${e.message}"
                isLoading = false
            }
        }
    }

    LaunchedEffect(currentEpIndex) {
        loadEpisodeVideo()
    }

    fun toggleOrientation() {
        if (isLandscape) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            isLandscape = false
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            isLandscape = true
        }
    }

    BackHandler {
        if (isLandscape) {
            toggleOrientation()
        } else {
            onBack()
        }
    }

    fun formatTime(ms: Long): String {
        val totalSec = (ms / 1000).toInt()
        val s = totalSec % 60
        val m = (totalSec / 60) % 60
        val h = totalSec / 3600
        return if (h > 0) String.format("%02d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
    }

    if (showQualityDialog) {
        QualitySelectionDialog(
            currentQuality = selectedQuality,
            onQualitySelected = { q ->
                selectedQuality = q
                prefs.preferredQuality = q
                val cand = candidates.firstOrNull { it.quality == q } ?: candidates.firstOrNull()
                if (cand != null) {
                    playUrl(cand.url)
                }
            },
            onDismiss = { showQualityDialog = false }
        )
    }

    Scaffold(
        containerColor = if (isLandscape) Color.Black else WibukuBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isLandscape) PaddingValues(0.dp) else padding)
        ) {
            // ── VIDEO PLAYER BOX ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isLandscape) Modifier.fillMaxHeight()
                        else Modifier
                            .aspectRatio(16f / 9f)
                            .background(Color.Black)
                    )
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { if (!isLocked) showControls = !showControls },
                                onDoubleTap = { offset ->
                                    if (!isLocked) {
                                        val width = size.width
                                        if (offset.x < width / 2) {
                                            exoPlayer.seekBack()
                                        } else {
                                            exoPlayer.seekForward()
                                        }
                                    }
                                }
                            )
                        }
                )

                // Buffering Spinner / Loading
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = WibukuPrimary, strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Menyiapkan video ($selectedQuality)...", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                // Error Overlay
                if (errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.85f))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(errorMessage ?: "Terjadi kesalahan", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { loadEpisodeVideo() },
                                    colors = ButtonDefaults.buttonColors(containerColor = WibukuPrimary)
                                ) {
                                    Text("Coba Lagi", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { showQualityDialog = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                ) {
                                    Text("Pilih Resolusi", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Lock Overlay (Buka Kunci)
                if (isLocked) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Surface(
                            modifier = Modifier
                                .padding(16.dp)
                                .align(Alignment.TopStart)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { isLocked = false },
                            color = Color.Black.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, WibukuPrimary)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.LockOpen, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Buka Kunci", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // In-Screen Video Controls Overlay
                if (showControls && !isLocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.42f))
                    ) {
                        // Top Bar: Back, Title, Lock, Resolution Button [⚙️ 360p/720p/1080p], Speed
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { if (isLandscape) toggleOrientation() else onBack() }) {
                                Icon(Icons.Rounded.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                            }

                            Text(
                                text = currentEp.title,
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(onClick = { isLocked = true }) {
                                Icon(Icons.Rounded.Lock, contentDescription = "Kunci", tint = Color.White, modifier = Modifier.size(20.dp))
                            }

                            // ── TOMBOL RESOLUSI LENGKAP ALA YOUTUBE [⚙️ 360p / 1080p] ──
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showQualityDialog = true },
                                color = Color.Black.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, WibukuPrimary.copy(alpha = 0.85f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = selectedQuality,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Speed button
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val idx = speedList.indexOf(playbackSpeed)
                                        val next = speedList[(idx + 1) % speedList.size]
                                        playbackSpeed = next
                                        exoPlayer.setPlaybackSpeed(next)
                                    },
                                color = Color.Black.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.24f))
                            ) {
                                Text(
                                    text = "${playbackSpeed}x",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Center Controls: Prev, -10s, Play/Pause/Buffer Spinner, +10s, Next
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Prev ep
                            val hasPrev = currentEpIndex < episodes.size - 1
                            IconButton(
                                onClick = { if (hasPrev) currentEpIndex += 1 },
                                enabled = hasPrev
                            ) {
                                Icon(
                                    Icons.Rounded.SkipPrevious,
                                    contentDescription = "Prev Ep",
                                    tint = if (hasPrev) Color.White else Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            // -10s
                            IconButton(onClick = { exoPlayer.seekBack() }) {
                                Icon(Icons.Rounded.Replay10, contentDescription = "-10s", tint = Color.White, modifier = Modifier.size(32.dp))
                            }

                            // Play / Pause / Dynamic Buffer Spinner
                            if (isBuffering) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                    },
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Rounded.PauseCircleFilled else Icons.Rounded.PlayCircleFilled,
                                        contentDescription = "Play/Pause",
                                        tint = Color.White,
                                        modifier = Modifier.size(56.dp)
                                    )
                                }
                            }

                            // +10s
                            IconButton(onClick = { exoPlayer.seekForward() }) {
                                Icon(Icons.Rounded.Forward10, contentDescription = "+10s", tint = Color.White, modifier = Modifier.size(32.dp))
                            }

                            // Next ep
                            val hasNext = currentEpIndex > 0
                            IconButton(
                                onClick = { if (hasNext) currentEpIndex -= 1 },
                                enabled = hasNext
                            ) {
                                Icon(
                                    Icons.Rounded.SkipNext,
                                    contentDescription = "Next Ep",
                                    tint = if (hasNext) Color.White else Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        // Bottom Scrubber Bar & Fullscreen Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = formatTime(currentPositionMs), color = Color.White, fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Slider(
                                value = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
                                onValueChange = { frac ->
                                    val target = (frac * durationMs).toLong()
                                    currentPositionMs = target
                                    exoPlayer.seekTo(target)
                                },
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = WibukuPrimary,
                                    activeTrackColor = WibukuPrimary,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = formatTime(durationMs), color = Color.White, fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(onClick = { toggleOrientation() }) {
                                Icon(
                                    imageVector = if (isLandscape) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                                    contentDescription = "Fullscreen",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // ── DAFTAR EPISODE DI BAWAH (HANYA MUNCUL DI MODE POTRET) ──
            if (!isLandscape) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = WibukuSurface,
                    shadowElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(
                            text = animeTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Sedang memutar: ${currentEp.title}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WibukuPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(episodes) { index, ep ->
                        val isCurrent = index == currentEpIndex
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    if (!isCurrent) currentEpIndex = index
                                },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) WibukuBadge else WibukuSurface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isCurrent) Icons.Rounded.PlayCircleFilled else Icons.Rounded.PlayCircleOutline,
                                    contentDescription = null,
                                    tint = if (isCurrent) WibukuPrimary else WibukuMuted,
                                    modifier = Modifier.size(24.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ep.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isCurrent) WibukuPrimary else WibukuText,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
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
                        }
                    }
                }
            }
        }
    }
}
