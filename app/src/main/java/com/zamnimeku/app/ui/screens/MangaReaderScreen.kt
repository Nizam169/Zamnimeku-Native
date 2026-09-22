package com.zamnimeku.app.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.zamnimeku.app.data.api.MangaApi
import com.zamnimeku.app.ui.components.ErrorView
import com.zamnimeku.app.ui.components.LoadingView
import com.zamnimeku.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaReaderScreen(
    chapterSlug: String,
    chapterTitle: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var images by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showControls by remember { mutableStateOf(true) }
    var retryTrigger by remember { mutableStateOf(0) }
    // Mode full-halaman: 1 nomor = 1 layar penuh, seluruh foto kelihatan (Fit).
    // Bisa diganti ke mode scroll panjang lewat tombol di top bar.
    var pagerMode by remember { mutableStateOf(true) }

    LaunchedEffect(chapterSlug, retryTrigger) {
        isLoading = true
        errorMessage = null
        try {
            val list = MangaApi.getChapterImages(chapterSlug)
            images = list
            if (list.isEmpty()) errorMessage = "Gambar chapter tidak ditemukan."
        } catch (e: Exception) {
            errorMessage = "Gagal memuat gambar: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    val listState = rememberLazyListState()
    val firstVisibleItemIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    val pagerState = rememberPagerState(pageCount = { images.size })

    // Layar jangan mati/kunci sendiri selama baca komik
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    // Nomor halaman yang tampil di pill bawah
    val displayIndex = if (pagerMode) {
        if (images.isNotEmpty()) pagerState.currentPage else 0
    } else {
        firstVisibleItemIndex
    }

    Scaffold(
        containerColor = Color(0xFF1E293B)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { showControls = !showControls })
                }
        ) {
            if (isLoading) {
                LoadingView(text = "Memuat halaman komik...")
            } else if (errorMessage != null) {
                ErrorView(
                    message = errorMessage ?: "Terjadi kesalahan",
                    onRetry = { retryTrigger++ }
                )
            } else if (pagerMode) {
                // MODE FULL-HALAMAN: 1 nomor = 1 layar penuh, geser kiri-kanan.
                // Fit = seluruh foto kelihatan utuh dalam layar.
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val imgUrl = images[page]
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imgUrl)
                                .crossfade(true)
                                .addHeader("Referer", "https://www.mynimeku.com/")
                                .build(),
                            contentDescription = "Halaman ${page + 1}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                            loading = {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = WibukuPrimary, strokeWidth = 2.5.dp, modifier = Modifier.size(28.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Halaman ${page + 1}",
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            },
                            error = {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Gagal memuat halaman ${page + 1}", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                }
                            }
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    // Tanpa padding/seam: halaman nempel full-bleed selebar layar
                    contentPadding = PaddingValues(0.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    itemsIndexed(images) { index, imgUrl ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A))
                        ) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(imgUrl)
                                    .crossfade(true)
                                    .addHeader("Referer", "https://www.mynimeku.com/")
                                    .build(),
                                contentDescription = "Halaman ${index + 1}",
                                // FillWidth + TopCenter = selebar layar, disambung dari atas
                                contentScale = ContentScale.FillWidth,
                                alignment = Alignment.TopCenter,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                                loading = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(300.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(color = WibukuPrimary, strokeWidth = 2.5.dp, modifier = Modifier.size(28.dp))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Halaman ${index + 1}",
                                                color = Color.White.copy(alpha = 0.6f),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                },
                                error = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Gagal memuat halaman ${index + 1}", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Top Bar Overlay
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Black.copy(alpha = 0.8f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = chapterTitle,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        // Ganti mode: full-halaman per nomor <-> scroll panjang
                        IconButton(onClick = { pagerMode = !pagerMode }) {
                            Icon(
                                Icons.Rounded.SwapVert,
                                contentDescription = if (pagerMode) "Mode scroll" else "Mode full-halaman",
                                tint = if (pagerMode) WibukuPrimary else Color.White
                            )
                        }
                    }
                }
            }

            // Bottom Page Indicator Pill
            if (images.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .align(Alignment.BottomCenter),
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WibukuPrimary.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "${displayIndex + 1} / ${images.size}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
