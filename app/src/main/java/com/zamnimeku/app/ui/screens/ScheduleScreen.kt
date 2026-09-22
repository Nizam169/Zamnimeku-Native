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
import com.zamnimeku.app.data.api.OtakuApi
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

    private fun todayIndex(): Int {
        return (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)
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
                val real = withContext(Dispatchers.IO) { OtakuApi.getSchedule() }
                if (real.isEmpty()) {
                    _errorMessage.value = "Jadwal tidak ditemukan."
                    _scheduleMap.value = emptyMap()
                } else {
                    val today = todayIndex()
                    val dateByDay = _days.value.associate { it.dayIndex to it.dateNum }
                    val nameByDay = _days.value.associate { it.dayIndex to it.fullName }
                    val mapped = mutableMapOf<Int, MutableList<AnimeSchedule>>()
                    for (day in real) {
                        val idx = dayNameToIndex(day.day)
                        if (idx == -1) continue
                        val dayName = nameByDay[idx] ?: day.day
                        val dateNum = dateByDay[idx] ?: 0
                        // Status tayang ngikutin hari: hari <= hari ini = sudah tayang
                        val aired = idx <= today
                        val list = day.animes.map { card ->
                            AnimeSchedule(
                                title = card.title,
                                slug = card.slug,
                                episode = "Setiap $dayName",
                                time = if (dateNum > 0) "Tgl $dateNum" else "-",
                                views = "-",
                                rating = "-",
                                posterUrl = "",
                                isAired = aired
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
                        _errorMessage.value = "Jadwal tidak ditemukan."
                    } else {
                        // Ambil foto + info episode di background untuk hari ini
                        fetchDetailsForDay(_selectedDayIndex.value)
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat jadwal: ${e.message}"
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
                                val d = OtakuApi.getAnimeDetail(item.slug)
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

    private fun generateSchedules() {
        val dummyData = mapOf(
            0 to listOf( // Minggu
                AnimeSchedule("One Piece", "one-piece-sub-indo", "Episode 1122", "09:30", "154.2K", "8.92", "https://otakudesu.blog/wp-content/uploads/2021/08/One-Piece-Sub-Indo.jpg", true),
                AnimeSchedule("Shangri-La Frontier Season 2", "shangri-la-frontier-s2-sub-indo", "Episode 18", "16:30", "42.8K", "8.14", "https://otakudesu.blog/wp-content/uploads/2024/10/Shangri-La-Frontier-Season-2-Sub-Indo.jpg", true),
                AnimeSchedule("Ranma 1/2 (2024)", "ranma-1-2-2024-sub-indo", "Episode 11", "23:55", "18.5K", "7.65", "https://otakudesu.blog/wp-content/uploads/2024/10/Ranma-1-2-2024-Sub-Indo.jpg", true),
                AnimeSchedule("Blue Box (Ao no Hako)", "blue-box-sub-indo", "Episode 22", "22:30", "56.1K", "8.45", "https://otakudesu.blog/wp-content/uploads/2024/10/Ao-no-Hako-Sub-Indo.jpg", true),
                AnimeSchedule("MF Ghost Season 2", "mf-ghost-season-2-sub-indo", "Episode 10", "23:00", "28.9K", "7.88", "https://otakudesu.blog/wp-content/uploads/2024/10/MF-Ghost-Season-2-Sub-Indo.jpg", false),
                AnimeSchedule("Himitsu no AiPri", "himitsu-no-aipri-sub-indo", "Episode 48", "10:00", "8.4K", "6.94", "https://otakudesu.blog/wp-content/uploads/2024/04/Himitsu-no-AiPri-Sub-Indo.jpg", true)
            ),
            1 to listOf( // Senin
                AnimeSchedule("Bleach: Sennen Kessen-hen Season 3", "bleach-thousand-year-blood-war-s3-sub-indo", "Episode 9", "22:00", "88.3K", "8.85", "https://otakudesu.blog/wp-content/uploads/2024/10/Bleach-Thousand-Year-Blood-War-Part-3-Sub-Indo.jpg", true),
                AnimeSchedule("Tower of God Season 2", "tower-of-god-s2-sub-indo", "Episode 23", "21:00", "39.5K", "7.52", "https://otakudesu.blog/wp-content/uploads/2024/07/Tower-of-God-Season-2-Sub-Indo.jpg", true),
                AnimeSchedule("Natsume Yuujinchou Shichi", "natsume-yuujinchou-s7-sub-indo", "Episode 11", "23:00", "19.1K", "8.65", "https://otakudesu.blog/wp-content/uploads/2024/10/Natsume-Yuujinchou-Shichi-Sub-Indo.jpg", true),
                AnimeSchedule("Kamonohashi Ron no Kindan Suiri S2", "kamonohashi-ron-s2-sub-indo", "Episode 11", "21:30", "14.2K", "7.71", "https://otakudesu.blog/wp-content/uploads/2024/10/Kamonohashi-Ron-no-Kindan-Suiri-Season-2-Sub-Indo.jpg", true),
                AnimeSchedule("Tsuma, Shougakusei ni Naru.", "tsuma-shougakusei-sub-indo", "Episode 11", "20:30", "12.8K", "7.40", "https://otakudesu.blog/wp-content/uploads/2024/10/Tsuma-Shougakusei-ni-Naru-Sub-Indo.jpg", false),
                AnimeSchedule("Highspeed Etoile", "highspeed-etoile-sub-indo", "Episode 12", "23:45", "6.2K", "6.50", "https://otakudesu.blog/wp-content/uploads/2024/04/Highspeed-Etoile-Sub-Indo.jpg", false)
            ),
            2 to listOf( // Selasa
                AnimeSchedule("DanMachi Season 5", "dungeon-ni-deai-s5-sub-indo", "Episode 11", "20:00", "92.4K", "8.48", "https://otakudesu.blog/wp-content/uploads/2024/10/DanMachi-Season-5-Sub-Indo.jpg", true),
                AnimeSchedule("Amagami-san Chi no Enmusubi", "amagami-san-sub-indo", "Episode 12", "22:30", "27.3K", "7.35", "https://otakudesu.blog/wp-content/uploads/2024/10/Amagami-san-Chi-no-Enmusubi-Sub-Indo.jpg", true),
                AnimeSchedule("Hitoribocchi no Isekai Kouryakuhou", "hitoribocchi-isekai-sub-indo", "Episode 12", "23:00", "34.1K", "7.10", "https://otakudesu.blog/wp-content/uploads/2024/09/Hitoribocchi-no-Isekai-Kouryakuhou-Sub-Indo.jpg", true),
                AnimeSchedule("NegaPosi Angler", "negaposi-angler-sub-indo", "Episode 11", "21:00", "9.7K", "7.22", "https://otakudesu.blog/wp-content/uploads/2024/10/NegaPosi-Angler-Sub-Indo.jpg", false),
                AnimeSchedule("Tasogare Out Focus", "tasogare-out-focus-sub-indo", "Episode 12", "22:00", "11.5K", "7.15", "https://otakudesu.blog/wp-content/uploads/2024/07/Tasogare-Out-Focus-Sub-Indo.jpg", false),
                AnimeSchedule("Shy Season 2", "shy-season-2-sub-indo", "Episode 12", "23:30", "15.8K", "7.29", "https://otakudesu.blog/wp-content/uploads/2024/07/SHY-Season-2-Sub-Indo.jpg", false)
            ),
            3 to listOf( // Rabu
                AnimeSchedule("Re:Zero Season 3", "re-zero-kara-hajimeru-isekai-seikatsu-s3-sub-indo", "Episode 8", "21:30", "168.0K", "8.95", "https://otakudesu.blog/wp-content/uploads/2024/10/ReZero-Season-3-Sub-Indo.jpg", true),
                AnimeSchedule("Dragon Ball Daima", "dragon-ball-daima-sub-indo", "Episode 10", "22:40", "75.4K", "8.12", "https://otakudesu.blog/wp-content/uploads/2024/10/Dragon-Ball-Daima-Sub-Indo.jpg", true),
                AnimeSchedule("Across the Sky", "across-the-sky-sub-indo", "Episode 10", "19:00", "16.2K", "7.44", "https://otakudesu.blog/wp-content/uploads/2024/10/Haigakura-Sub-Indo.jpg", true),
                AnimeSchedule("Kimi wa Meido-sama", "kimi-wa-meido-sama-sub-indo", "Episode 11", "23:30", "31.9K", "7.40", "https://otakudesu.blog/wp-content/uploads/2024/10/Kimi-wa-Meido-sama-Sub-Indo.jpg", true),
                AnimeSchedule("Sengoku Youko Part 2", "sengoku-youko-part-2-sub-indo", "Episode 21", "23:00", "18.3K", "7.92", "https://otakudesu.blog/wp-content/uploads/2024/07/Sengoku-Youko-Senma-Konton-hen-Sub-Indo.jpg", false),
                AnimeSchedule("Murai no Koi", "murai-no-koi-sub-indo", "Episode 12", "20:30", "8.9K", "7.08", "https://otakudesu.blog/wp-content/uploads/2024/10/Murai-no-Koi-Sub-Indo.jpg", false)
            ),
            4 to listOf( // Kamis
                AnimeSchedule("Dandadan", "dandadan-sub-indo", "Episode 11", "23:00", "195.4K", "8.80", "https://otakudesu.blog/wp-content/uploads/2024/10/Dandadan-Sub-Indo.jpg", true),
                AnimeSchedule("Rurouni Kenshin (2023) S2", "rurouni-kenshin-kyoto-souran-sub-indo", "Episode 11", "23:55", "41.7K", "8.05", "https://otakudesu.blog/wp-content/uploads/2024/10/Rurouni-Kenshin-Kyoto-Souran-Sub-Indo.jpg", true),
                AnimeSchedule("Trillion Game", "trillion-game-sub-indo", "Episode 12", "22:30", "22.6K", "7.68", "https://otakudesu.blog/wp-content/uploads/2024/10/Trillion-Game-Sub-Indo.jpg", true),
                AnimeSchedule("Mecha-Ude", "mecha-ude-sub-indo", "Episode 11", "21:30", "13.4K", "7.20", "https://otakudesu.blog/wp-content/uploads/2024/10/Mecha-Ude-Sub-Indo.jpg", false),
                AnimeSchedule("Hoshifuru Oukoku no Nina", "hoshifuru-oukoku-no-nina-sub-indo", "Episode 11", "21:00", "19.8K", "7.55", "https://otakudesu.blog/wp-content/uploads/2024/10/Hoshifuru-Oukoku-no-Nina-Sub-Indo.jpg", false),
                AnimeSchedule("Yozakura-san Chi no Daisakusen", "yozakura-san-sub-indo", "Episode 26", "17:00", "25.1K", "7.45", "https://otakudesu.blog/wp-content/uploads/2024/04/Yozakura-san-Chi-no-Daisakusen-Sub-Indo.jpg", false)
            ),
            5 to listOf( // Jumat
                AnimeSchedule("Blue Lock Season 2", "blue-lock-vs-u-20-japan-sub-indo", "Episode 11", "22:30", "148.6K", "8.32", "https://otakudesu.blog/wp-content/uploads/2024/10/Blue-Lock-vs-U-20-Japan-Sub-Indo.jpg", true),
                AnimeSchedule("Sword Art Online Alternative: GGO II", "sao-alternative-gun-gale-online-ii-sub-indo", "Episode 11", "23:30", "64.2K", "7.78", "https://otakudesu.blog/wp-content/uploads/2024/10/Sword-Art-Online-Alternative-Gun-Gale-Online-II-Sub-Indo.jpg", true),
                AnimeSchedule("Mahoutsukai ni Narenakatta", "mahoutsukai-ni-narenakatta-sub-indo", "Episode 11", "20:30", "8.1K", "6.85", "https://otakudesu.blog/wp-content/uploads/2024/10/Mahoutsukai-ni-Narenakatta-Onnanoko-no-Hanashi-Sub-Indo.jpg", true),
                AnimeSchedule("Goukon ni Ittara Onna ga Inakatta", "goukon-ni-ittara-sub-indo", "Episode 11", "23:00", "15.7K", "7.38", "https://otakudesu.blog/wp-content/uploads/2024/10/Goukon-ni-Ittara-Onna-ga-Inakatta-Hanashi-Sub-Indo.jpg", false),
                AnimeSchedule("Raise wa Tanin ga Ii", "raise-wa-tanin-ga-ii-sub-indo", "Episode 11", "22:00", "38.4K", "7.92", "https://otakudesu.blog/wp-content/uploads/2024/10/Raise-wa-Tanin-ga-Ii-Sub-Indo.jpg", false),
                AnimeSchedule("Fairy Tail: 100 Years Quest", "fairy-tail-100-years-quest-sub-indo", "Episode 23", "16:30", "71.0K", "7.96", "https://otakudesu.blog/wp-content/uploads/2024/07/Fairy-Tail-100-Years-Quest-Sub-Indo.jpg", false)
            ),
            6 to listOf( // Sabtu
                AnimeSchedule("Bleach: Thousand-Year Blood War S3", "bleach-thousand-year-blood-war-s3-sub-indo", "Episode 10", "22:00", "112.5K", "8.85", "https://otakudesu.blog/wp-content/uploads/2024/10/Bleach-Thousand-Year-Blood-War-Part-3-Sub-Indo.jpg", true),
                AnimeSchedule("Dandadan", "dandadan-sub-indo", "Episode 11", "23:00", "195.4K", "8.80", "https://otakudesu.blog/wp-content/uploads/2024/10/Dandadan-Sub-Indo.jpg", true),
                AnimeSchedule("Kagaku x Bouken Survival!", "kagaku-x-bouken-survival-sub-indo", "Episode 11", "17:35", "5.8K", "6.70", "https://otakudesu.blog/wp-content/uploads/2024/10/Kagaku-x-Bouken-Survival-Sub-Indo.jpg", true),
                AnimeSchedule("Youkai Gakkou no Sensei Hajimemashita!", "youkai-gakkou-no-sensei-sub-indo", "Episode 11", "22:30", "14.6K", "7.18", "https://otakudesu.blog/wp-content/uploads/2024/10/Youkai-Gakkou-no-Sensei-Hajimemashita-Sub-Indo.jpg", false),
                AnimeSchedule("Sayounara Ryuusei, Konnichiwa Jinsei", "sayounara-ryuusei-sub-indo", "Episode 11", "21:00", "22.1K", "6.95", "https://otakudesu.blog/wp-content/uploads/2024/10/Sayounara-Ryuusei-Konnichiwa-Jinsei-Sub-Indo.jpg", false),
                AnimeSchedule("Kabushikigaisha Magi-Lumiere", "kabushikigaisha-magilumiere-sub-indo", "Episode 11", "21:30", "16.8K", "7.42", "https://otakudesu.blog/wp-content/uploads/2024/10/Kabushikigaisha-Magi-Lumiere-Sub-Indo.jpg", false)
            )
        )
        _scheduleMap.value = dummyData
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
                        text = "Jadwal Tayang",
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
                isLoading -> LoadingView(text = "Memuat jadwal rilis...")
                errorMessage != null && currentAnimeList.isEmpty() -> ErrorView(
                    message = errorMessage ?: "Gagal memuat jadwal",
                    onRetry = { viewModel.retry() }
                )
                currentAnimeList.isEmpty() -> ErrorView(
                    message = "Belum ada jadwal untuk hari ini.",
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
