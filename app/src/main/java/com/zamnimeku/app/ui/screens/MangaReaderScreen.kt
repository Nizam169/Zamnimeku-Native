package com.zamnimeku.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
    var images by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showControls by remember { mutableStateOf(true) }

    LaunchedEffect(chapterSlug) {
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

    Scaffold(
        containerColor = Color.Black
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
                ErrorView(message = errorMessage ?: "Terjadi kesalahan", onRetry = { /* reload */ })
            } else {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(images) { index, imgUrl ->
                        AsyncImage(
                            model = imgUrl,
                            contentDescription = "Halaman ${index + 1}",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                        )
                    }
                }
            }

            // Top Bar Overlay
            if (showControls) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = Color.Black.copy(alpha = 0.75f)
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
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
