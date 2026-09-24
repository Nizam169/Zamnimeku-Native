package com.zamnimeku.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.zamnimeku.app.data.api.AnimeApi
import com.zamnimeku.app.ui.components.ErrorView
import com.zamnimeku.app.ui.components.LoadingView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

// ─── PALET WARNA RESMI TEMA TERANG ──────────────────────────────────────────
private val ColorBgScreen = Color(0xFFFFFFFF)
private val ColorBgCard = Color(0xFFF5FAFE)
private val ColorPrimary = Color(0xFF29B6F6)
private val ColorPrimaryContainer = Color(0xFFE1F3FD)
private val ColorTextPrimary = Color(0xFF1E293B)
private val ColorTextSecondary = Color(0xFF64748B)
private val ColorDivider = Color(0xFFE2E8F0)
private val ColorStar = Color(0xFFFFC107)

// ─── DATA MODELS ────────────────────────────────────────────────────────────
data class AnimeSchedule(
    val title: String,
    val slug: String,
    val episode: String,
    val time: String,
    val views: String,
    val rating: String,
    val posterUrl: String,
    val isAired: Boolean
)

data class DayItem(
    val shortName: String,
    val fullName: String,
    val dateNum: Int,
    val dayIndex: Int
)

// ─── VIEWMODEL ──────────────────────────────────────────────────────────────
class ScheduleViewModel : ViewModel() {
    private val _selectedDayIndex = MutableStateFlow(0)
    val selectedDayIndex: StateFlow<Int> = _selectedDayIndex.asStateFlow()

    private val _scheduleMap = MutableStateFlow<Map<Int, List<AnimeSchedule>>>(emptyMap())
    val scheduleMap: StateFlow<Map<Int, List<AnimeSchedule>>> = _scheduleMap.asStateFlow()

    private val _days = MutableStateFlow<List<DayItem>>(emptyList())
    val days: StateFlow<List<DayItem>> = _days.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        initDays()
        loadRealSchedule()
    }

    private fun initDays() {
        val calendar = Calendar.getInstance()
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1=Sunday, 2=Monday, ...

        val dayNames = listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")
        val fullDayNames = listOf("Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")

        val list = mutableListOf<DayItem>()
        // Buat 7 hari mulai dari hari ini atau awal pekan
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        for (i in 0..6) {
            list.add(
                DayItem(
                    shortName = dayNames[i],
                    fullName = fullDayNames[i],
                    dateNum = calendar.get(Calendar.DAY_OF_MONTH),
                    dayIndex = i
                )
            )
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        _days.value = list

        // Set default ke hari ini (0..6)
        _selectedDayIndex.value = (currentDayOfWeek - 1).coerceIn(0, 6)
    }

    fun selectDay(index: Int) {
        val normalized = (index + 7) % 7
        _selectedDayIndex.value = normalized
        fetchDetailsForDay(normalized)
    }

    fun retry() {
        loadRealSchedule()
    }

    private fun dayNameToIndex(name: String): Int {
        val n = name.lowercase()
        return when {
            n.contains("minggu") || n.contains("sunday") -> 0
            n.contains("senin") || n.contains("monday") -> 1
            n.contains("selasa") || n.contains("tuesday") -> 2
            n.contains("rabu") || n.contains("wednes") -> 3
            n.contains("kamis") || n.contains("thurs") -> 4
            n.contains("jumat") || n.contains("friday") -> 5
            n.contains("sabtu") || n.contains("satur") -> 6
            else -> -1
        }
    }

    private val thumbCache = mutableMapOf<String, String>()
    private val scoreCache = mutableMapOf<String, String>()
    private val totalEpCache = mutableMapOf<String, String>()

    private fun loadRealSchedule() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val real = withContext(Dispatchers.IO) { AnimeApi.getSchedule() }
                if (real.isEmpty()) {
                    _errorMessage.value = "Update series tidak ditemukan."
                    _scheduleMap.value = emptyMap()
                } else {
                    val mapped = mutableMapOf<Int, MutableList<AnimeSchedule>>()
                    for (day in real) {
                        val idx = dayNameToIndex(day.day)
                        if (idx == -1) continue
                        val list = day.animes.map { card ->
                            AnimeSchedule(
                                title = card.title,
                                slug = card.slug,
                                episode = card.episode.ifEmpty { "Episode baru" },
                                time = card.date.ifEmpty { "Update" },
                                views = "-",
                                rating = card.score.ifEmpty { "-" },
                                posterUrl = card.thumb,
                                isAired = true
                            )
                        }
                        mapped.getOrPut(idx) { mutableListOf() }.addAll(list)
                    }
                    // Deduplikasi per hari (satu anime bisa muncul 2x di API)
                    mapped.keys.forEach { k ->
                        mapped[k] = mapped[k]!!.distinctBy { it.slug }.toMutableList()
                    }
                    _scheduleMap.value = mapped
                    if (mapped.isEmpty()) {
                        _errorMessage.value = "Update series tidak ditemukan."
                    } else {
                        // Ambil foto + info episode di background untuk hari ini
                        fetchDetailsForDay(_selectedDayIndex.value)
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat update series: ${e.message}"
                _scheduleMap.value = emptyMap()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun fetchDetailsForDay(dayIdx: Int) {
        val current = _scheduleMap.value[dayIdx] ?: return
        val missing = current.filter { it.posterUrl.isEmpty() && !thumbCache.containsKey(it.slug) }
        if (missing.isEmpty()) return
        viewModelScope.launch {
            try {
                val results = withContext(Dispatchers.IO) {
                    missing.take(12).map { item ->
                        async {
                            try {
                                val d = AnimeApi.getAnimeDetail(item.slug)
                                if (d != null) {
                                    thumbCache[item.slug] = d.thumb
                                    scoreCache[item.slug] = d.score
                                    totalEpCache[item.slug] = d.totalEpisodes
                                }
                            } catch (_: Exception) {}
                        }
                    }.awaitAll()
                }
                // Tempel hasil ke map supaya foto + tanggal update muncul
                val updated = _scheduleMap.value.toMutableMap()
                val dayName = _days.value.getOrNull(dayIdx)?.fullName ?: ""
                val list = (updated[dayIdx] ?: return@launch).map { item ->
                    val thumb = thumbCache[item.slug] ?: ""
                    val score = scoreCache[item.slug] ?: "-"
                    val total = totalEpCache[item.slug] ?: ""
                    val validTotal = total.isNotEmpty() && total != "?" && total != "-" &&
                        !total.equals("Unknown", ignoreCase = true)
                    val epText = if (validTotal) {
                        "Total $total Ep • Setiap $dayName"
                    } else {
                        item.episode
                    }
                    item.copy(
                        posterUrl = thumb,
                        rating = score,
                        views = if (total.isNotEmpty()) total else "-",
                        episode = epText
                    )
                }
                updated[dayIdx] = list.toMutableList()
                _scheduleMap.value = updated
            } catch (_: Exception) {}
        }
    }
}

// ─── MAIN COMPOSABLE: SCHEDULE SCREEN ───────────────────────────────────────
@Composable
fun ScheduleScreen(
    onAnimeClick: (String, String, String) -> Unit,
    viewModel: ScheduleViewModel = viewModel()
) {
    val selectedDayIndex by viewModel.selectedDayIndex.collectAsState()
    val scheduleMap by viewModel.scheduleMap.collectAsState()
    val days by viewModel.days.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val currentAnimeList = scheduleMap[selectedDayIndex] ?: emptyList()

    val prevDayName = days.getOrNull((selectedDayIndex - 1 + 7) % 7)?.fullName ?: "Kemarin"
    val nextDayName = days.getOrNull((selectedDayIndex + 1) % 7)?.fullName ?: "Besok"

    Scaffold(
        containerColor = ColorBgScreen,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(ColorBgScreen)
            ) {
                // 1. HEADER (Teks Tengah, Bold, 22sp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Update Series",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextPrimary,
                        textAlign = TextAlign.Center
                    )
                }

                // Divider tipis di bawah header
                HorizontalDivider(thickness = 1.dp, color = ColorDivider)

                // 2. DAY SELECTOR (7 Hari: Min - Sab)
                DaySelector(
                    days = days,
                    selectedIndex = selectedDayIndex,
                    onDaySelected = { index -> viewModel.selectDay(index) }
                )

                HorizontalDivider(thickness = 1.dp, color = ColorDivider)
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(ColorBgScreen)
        ) {
            when {
                isLoading -> LoadingView(text = "Memuat update series...")
                errorMessage != null && currentAnimeList.isEmpty() -> ErrorView(
                    message = errorMessage ?: "Gagal memuat update series",
                    onRetry = { viewModel.retry() }
                )
                currentAnimeList.isEmpty() -> ErrorView(
                    message = "Belum ada update series.",
                    onRetry = { viewModel.retry() }
                )
                else -> {
            // 3. LIST ANIME (LazyColumn, spasi 12dp, padding 16dp)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(currentAnimeList, key = { it.title + it.time }) { anime ->
                    AnimeScheduleCard(
                        anime = anime,
                        onClick = { onAnimeClick(anime.slug, anime.title, anime.posterUrl) }
                    )
                }
            }
                }
            }

            // 4. TOMBOL MELAYANG NAVIGASI HARI (Pill Kiri & Kanan)
            FloatingDayNav(
                prevDayText = "← $prevDayName",
                nextDayText = "$nextDayName →",
                onPrevClick = { viewModel.selectDay(selectedDayIndex - 1) },
                onNextClick = { viewModel.selectDay(selectedDayIndex + 1) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
    }
}

// ─── 1. COMPOSABLE: DAY SELECTOR (7 Hari Min-Sab) ───────────────────────────
@Composable
fun DaySelector(
    days: List<DayItem>,
    selectedIndex: Int,
    onDaySelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        days.forEachIndexed { index, day ->
            val isSelected = index == selectedIndex

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onDaySelected(index) }
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Singkatan Hari di Atas
                Text(
                    text = day.shortName,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) ColorPrimary else ColorTextSecondary
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Tanggal di Kotak Rounded 16dp
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) ColorPrimary else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${day.dateNum}",
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else ColorTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Titik Kecil Biru di Bawah Tanggal Terpilih
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) ColorPrimary else Color.Transparent)
                )
            }
        }
    }
}

// ─── 2. COMPOSABLE: ANIME SCHEDULE CARD ────────────────────────────────────
@Composable
fun AnimeScheduleCard(
    anime: AnimeSchedule,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = ColorPrimary.copy(alpha = 0.15f)
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ColorBgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Strip Aksen Vertikal di Sisi Kiri (#29B6F6)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(ColorPrimary)
            )

            // Kolom Kiri: Jam Tayang (Lebar ±72dp, bold, 20sp, rata tengah)
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = anime.time,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary,
                    textAlign = TextAlign.Center
                )
            }

            // Poster Anime (Rasio 3:4, rounded 12dp, lebar ±95dp)
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(95.dp)
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ColorPrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (anime.posterUrl.isNotEmpty()) {
                    AsyncImage(
                        model = anime.posterUrl,
                        contentDescription = anime.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Placeholder selagi foto detail dimuat
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = ColorPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Memuat...",
                            fontSize = 10.sp,
                            color = ColorTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Kolom Kanan: Info Anime
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 10.dp, bottom = 10.dp, end = 14.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // Judul Anime: Bold 17sp, max 1 baris, Ellipsis
                Text(
                    text = anime.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // "Episode XX": Abu-abu 14sp
                Text(
                    text = anime.episode,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorTextSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Baris Ikon Mata + Views & Ikon Bintang + Rating
                // (disembunyikan kalau datanya "-" atau mengandung unknown
                // biar gak bug tampil strip/unknown)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (anime.views.isNotEmpty() && anime.views != "-" &&
                        !anime.views.contains("unknown", ignoreCase = true)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Visibility,
                                contentDescription = "Views",
                                tint = ColorTextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${anime.views} Ep",
                                fontSize = 12.sp,
                                color = ColorTextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (anime.rating.isNotEmpty() && anime.rating != "-" &&
                        !anime.rating.contains("unknown", ignoreCase = true)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = "Rating",
                                tint = ColorStar,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = anime.rating,
                                fontSize = 12.sp,
                                color = ColorTextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Status: Titik Bulat Kecil + Teks ("Sudah Tayang" = Biru #29B6F6, "Belum Tayang" = Abu-abu)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (anime.isAired) ColorPrimary else ColorTextSecondary)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (anime.isAired) "Sudah Tayang" else "Belum Tayang",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (anime.isAired) ColorPrimary else ColorTextSecondary
                    )
                }
            }
        }
    }
}

// ─── 3. COMPOSABLE: TOMBOL MELAYANG NAVIGASI HARI ───────────────────────────
@Composable
fun FloatingDayNav(
    prevDayText: String,
    nextDayText: String,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pill Kiri: Hari Sebelumnya
        Surface(
            modifier = Modifier
                .shadow(6.dp, RoundedCornerShape(24.dp), spotColor = ColorPrimary.copy(alpha = 0.25f))
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, ColorPrimary.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                .clickable { onPrevClick() },
            color = Color.White,
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(
                text = prevDayText,
                color = ColorTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Pill Kanan: Hari Berikutnya
        Surface(
            modifier = Modifier
                .shadow(6.dp, RoundedCornerShape(24.dp), spotColor = ColorPrimary.copy(alpha = 0.25f))
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, ColorPrimary.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                .clickable { onNextClick() },
            color = Color.White,
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(
                text = nextDayText,
                color = ColorTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

// ─── PREVIEW ────────────────────────────────────────────────────────────────
@Preview(showBackground = true)
@Composable
fun ScheduleScreenPreview() {
    ScheduleScreen(onAnimeClick = { _, _, _ -> })
}
