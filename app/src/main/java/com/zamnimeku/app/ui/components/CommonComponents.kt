package com.zamnimeku.app.ui.components

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.zamnimeku.app.data.api.UpdateApi
import com.zamnimeku.app.data.model.AnimeCard
import com.zamnimeku.app.data.model.MangaCard
import com.zamnimeku.app.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

enum class NavTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Rounded.GridView),
    ANIME2("Anime2", Icons.Rounded.LiveTv),
    GENRE("Genre", Icons.Rounded.Category),
    SCHEDULE("Jadwal", Icons.Rounded.CalendarMonth),
    MANGA("Komik", Icons.Rounded.MenuBook),
    HISTORY("History", Icons.Rounded.History)
}

@Composable
fun AppBottomBar(
    currentTab: NavTab,
    onTabSelected: (NavTab) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
        color = Color.White,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFE3F2FD),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavTab.entries.forEach { tab ->
                        val isSelected = tab == currentTab
                        val bgColor by animateColorAsState(
                            targetValue = if (isSelected) WibukuPrimary else Color.Transparent,
                            animationSpec = tween(durationMillis = 200),
                            label = "navBg"
                        )
                        val contentColor by animateColorAsState(
                            targetValue = if (isSelected) Color.White else WibukuMuted,
                            animationSpec = tween(durationMillis = 200),
                            label = "navColor"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(bgColor)
                                .clickable { onTabSelected(tab) }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    tint = contentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = tab.title,
                                    color = contentColor,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
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
    // Pertahanan terakhir: teks berisi "unknown" (varian apa pun) tidak
    // pernah ditampilkan sebagai badge.
    fun shown(s: String): String =
        if (s.contains("unknown", ignoreCase = true)) "" else s

    val epBadge = shown(anime.episode)
    val dayBadge = shown(anime.day)
    val categoryBadge = shown(anime.category)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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

                // Subtle shadow gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)),
                                startY = 120f
                            )
                        )
                )

                // Episode badge (Top Start)
                if (epBadge.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .padding(6.dp)
                            .align(Alignment.TopStart),
                        color = WibukuPrimary.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = epBadge,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Score / Day badge (Bottom End)
                val tag = if (categoryBadge.isNotEmpty()) categoryBadge else dayBadge
                if (tag.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .padding(6.dp)
                            .align(Alignment.BottomEnd),
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = tag,
                            color = Color.White,
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
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(8.dp),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = WibukuText
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
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
                    .background(WibukuBadge),
                contentAlignment = Alignment.Center
            ) {
                if (manga.thumb.isNotEmpty()) {
                    AsyncImage(
                        model = manga.thumb,
                        contentDescription = manga.title,
                        // Fit = seluruh foto tampil, tidak kepotong.
                        // Latar WibukuBadge mengisi sisa ruang kalau rasio beda.
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.MenuBook,
                        contentDescription = null,
                        tint = WibukuMuted,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Text(
                text = manga.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(8.dp),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = WibukuText
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
            CircularProgressIndicator(color = WibukuPrimary, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(14.dp))
            Text(text = text, style = MaterialTheme.typography.bodyMedium, color = WibukuMuted, fontWeight = FontWeight.Medium)
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
                textAlign = TextAlign.Center,
                color = WibukuText
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = WibukuPrimary),
                shape = RoundedCornerShape(12.dp)
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
    onDismiss: () -> Unit,
    onServerSelected: (String, String) -> Unit = { _, _ -> },
    qualityUrls: Map<String, String> = emptyMap(),
    serverOptions: Map<String, List<Pair<String, String>>> = emptyMap(),
    // URL yang sedang diputar — opsi dengan sumber identik ditandai
    currentUrl: String = ""
) {
    val standardQualities = listOf(
        Pair("360p", "Hemat Kuota • Paling Cepat"),
        Pair("480p", "Kualitas Standar (SD) • Lancar"),
        Pair("720p", "High Definition (HD) • Jernih"),
        Pair("1080p", "Ultra Full HD (FHD) • Sangat Jernih")
    )
    val qualities = (standardQualities + qualityUrls.keys
        .filterNot { quality -> standardQualities.any { it.first == quality } }
        .map { quality -> quality to "Kualitas asli dari server" })
        .distinctBy { it.first }
    var expandedQuality by remember(currentQuality) { mutableStateOf(currentQuality) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WibukuDark,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.HighQuality, contentDescription = null, tint = WibukuPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Kualitas & Server", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val infoText = if (qualityUrls.isEmpty()) "Resolusi belum dimuat." else null
                if (infoText != null) {
                    Text(
                        text = infoText,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                qualities.forEach { (q, desc) ->
                    val isSelected = q == currentQuality
                    val itemUrl = qualityUrls[q]
                    val hasUrl = itemUrl != null
                    val servers = serverOptions[q].orEmpty()
                    // Sumber identik dengan yang sedang diputar
                    val sameSource = itemUrl != null && currentUrl.isNotEmpty() && itemUrl == currentUrl
                    val descText = when {
                        !hasUrl -> "Belum tersedia"
                        sameSource && !isSelected -> "$desc • sumber sama"
                        else -> desc
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = hasUrl) {
                                expandedQuality = q
                                onQualitySelected(q)
                                if (servers.size <= 1) onDismiss()
                            },
                        color = if (isSelected) WibukuPrimary.copy(alpha = 0.2f) else Color.White.copy(alpha = if (hasUrl) 0.08f else 0.04f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) WibukuPrimary else Color.Transparent)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = if (isSelected) WibukuPrimary else Color.White.copy(alpha = 0.2f),
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
                                Text(text = descText, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                            }
                            if (isSelected) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = WibukuPrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
                val expandedServers = serverOptions[expandedQuality].orEmpty()
                if (expandedServers.size > 1) {
                    Text(
                        text = "Server $expandedQuality",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        expandedServers.forEach { (server, url) ->
                            val isCurrent = currentUrl.isNotEmpty() && currentUrl == url
                            Surface(
                                modifier = Modifier.clickable {
                                    onServerSelected(expandedQuality, server)
                                    onDismiss()
                                },
                                color = if (isCurrent) WibukuPrimary else Color.White.copy(alpha = 0.09f),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isCurrent) WibukuPrimary else Color.White.copy(alpha = 0.12f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isCurrent) {
                                        Icon(
                                            Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = server,
                                        color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.8f),
                                        fontSize = 10.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

sealed interface UpdateDlState {
    data object Idle : UpdateDlState
    data class Downloading(val done: Long, val total: Long) : UpdateDlState
    data class Done(val file: File) : UpdateDlState
    data class Error(val msg: String) : UpdateDlState
}

private fun formatMB(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes / 1024f / 1024f
    return if (mb >= 10) "${mb.toInt()} MB" else String.format("%.1f MB", mb)
}

// Rapikan catatan rilis GitHub (markdown) jadi teks polos yang enak dibaca
// di dialog: buang heading/bold/code, ubah "- " jadi "• ",
// dan buang seksi "Cara install" (dialog sudah punya alurnya sendiri).
fun formatReleaseNotes(raw: String): String {
    if (raw.isBlank()) return "Ada update baru. Download dan install untuk dapat perbaikan terbaru."
    val out = mutableListOf<String>()
    var skipInstall = false
    for (rawLine in raw.lines()) {
        var s = rawLine.trim()
        s = s.replace(Regex("^#{1,6}\\s*"), "")
        s = s.replace("**", "").replace("`", "")
        if (s.isEmpty()) continue
        if (s.contains("cara install", ignoreCase = true)) {
            skipInstall = true
            continue
        }
        if (skipInstall) {
            // Akhir seksi install: header baru / bullet fitur
            if (s.startsWith("• ") || s.contains("Zamnimeku Native", ignoreCase = true)) {
                skipInstall = false
            } else {
                continue
            }
        }
        s = s.replace(Regex("^-\\s+"), "• ")
        s = s.replace(Regex("^\\d+\\.\\s+"), "• ")
        if (!s.startsWith("• ")) s = "• $s"
        out.add(s)
        if (out.size >= 6) break
    }
    if (out.isEmpty()) return "Ada update baru. Download dan install untuk dapat perbaikan terbaru."
    return out.joinToString("\n").take(350)
}

@Composable
fun UpdateDialog(
    currentVersion: String,
    newVersion: String,
    notes: String,
    tag: String,
    apkUrl: String,
    onLater: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dl by remember { mutableStateOf<UpdateDlState>(UpdateDlState.Idle) }
    var job by remember { mutableStateOf<Job?>(null) }

    fun destFile(): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
        if (!dir.exists()) dir.mkdirs()
        val safeTag = tag.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(dir, "Zamnimeku-$safeTag.apk")
    }

    fun startDownload() {
        if (job?.isActive == true) return
        val dest = destFile()
        // Lanjutkan file setengah jalan kalau ada
        job = scope.launch {
            dl = UpdateDlState.Downloading(dest.length(), -1L)
            val ok = UpdateApi.downloadApk(apkUrl, dest) { done, total ->
                dl = UpdateDlState.Downloading(done, total)
            }
            if (ok && dest.length() > 1024 * 1024) {
                dl = UpdateDlState.Done(dest)
            } else {
                if (dest.exists() && dest.length() <= 1024 * 1024) dest.delete()
                dl = UpdateDlState.Error("Download gagal. Cek koneksi lalu coba lagi.")
            }
        }
    }

    fun cancelDownload() {
        job?.cancel()
        job = null
        (dl as? UpdateDlState.Downloading)?.let { destFile().delete() }
        dl = UpdateDlState.Idle
    }

    fun openViaBrowser() {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)))
        } catch (_: Exception) {}
    }

    fun doInstall(file: File) {
        try {
            // Android 8+: harus diizinkan install dari sumber tak dikenal dulu
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !context.packageManager.canRequestPackageInstalls()
            ) {
                val settings = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(settings)
                return
            }
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
            val install = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(install)
        } catch (_: Exception) {
            openViaBrowser()
        }
    }

    AlertDialog(
        onDismissRequest = {
            if ((dl as? UpdateDlState.Downloading) != null) cancelDownload()
            onLater()
        },
        containerColor = WibukuSurface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.SystemUpdate, contentDescription = null, tint = WibukuPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Versi Baru Tersedia", color = WibukuText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "v$currentVersion → v$newVersion",
                    color = WibukuPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formatReleaseNotes(notes),
                    color = WibukuMuted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )

                // ── TIMELINE DOWNLOAD ──
                when (val state = dl) {
                    is UpdateDlState.Downloading -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        val frac = if (state.total > 0) {
                            (state.done.toFloat() / state.total.toFloat()).coerceIn(0f, 1f)
                        } else null
                        if (frac != null) {
                            LinearProgressIndicator(
                                progress = { frac },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = WibukuPrimary,
                                trackColor = WibukuBorder
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = WibukuPrimary,
                                trackColor = WibukuBorder
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (state.total > 0) {
                                val pct = ((state.done * 100) / state.total).toInt().coerceIn(0, 100)
                                "${formatMB(state.done)} / ${formatMB(state.total)} • $pct%"
                            } else {
                                "${formatMB(state.done)} terdownload..."
                            },
                            color = WibukuText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    is UpdateDlState.Done -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Download selesai (${formatMB(state.file.length())}). Install untuk mengganti versi lama.",
                            color = Color(0xFF10B981),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    is UpdateDlState.Error -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = state.msg,
                            color = Color(0xFFE74C3C),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            when (val state = dl) {
                is UpdateDlState.Downloading -> {
                    TextButton(onClick = { cancelDownload() }) {
                        Text(text = "Batal", color = Color(0xFFE74C3C), fontWeight = FontWeight.Bold)
                    }
                }
                is UpdateDlState.Done -> {
                    Button(
                        onClick = { doInstall(state.file) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Install Sekarang", fontWeight = FontWeight.Bold)
                    }
                }
                is UpdateDlState.Error -> {
                    Button(
                        onClick = { startDownload() },
                        colors = ButtonDefaults.buttonColors(containerColor = WibukuPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Coba Lagi", fontWeight = FontWeight.Bold)
                    }
                }
                else -> {
                    Button(
                        onClick = { startDownload() },
                        colors = ButtonDefaults.buttonColors(containerColor = WibukuPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Download Update", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = {
                    if ((dl as? UpdateDlState.Downloading) != null) cancelDownload()
                    onLater()
                }) {
                    Text(text = "Nanti", color = WibukuMuted, fontWeight = FontWeight.Bold)
                }
                if (dl is UpdateDlState.Error || dl is UpdateDlState.Done) {
                    TextButton(onClick = { openViaBrowser() }) {
                        Text(text = "Via Browser", color = WibukuPrimary, fontSize = 12.sp)
                    }
                }
            }
        }
    )
}
