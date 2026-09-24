package com.zamnimeku.app.data.model

enum class AnimeSource {
    OTAKUDESU,
    MYNIMEKU
}

data class AnimeCard(
    val title: String,
    val slug: String,
    val url: String,
    val thumb: String,
    val episode: String = "",
    val day: String = "",
    val date: String = "",
    val score: String = "",
    val category: String = "Anime"
)

data class Episode(
    val title: String,
    val slug: String,
    val url: String,
    val date: String = ""
)

data class VideoSource(
    val quality: String,
    val server: String,
    val url: String,
    val backupUrls: List<String> = emptyList(),
    val backupServers: List<String> = emptyList()
)

data class AnimeDetail(
    val title: String,
    val slug: String,
    val thumb: String,
    val synopsis: String,
    val score: String,
    val producer: String,
    val type: String,
    val status: String,
    val totalEpisodes: String,
    val duration: String,
    val releaseDate: String,
    val studio: String,
    val genres: List<String>,
    val episodes: List<Episode>
)

data class Genre(
    val name: String,
    val slug: String,
    val url: String
)

data class ScheduleDay(
    val day: String,
    val animes: List<AnimeCard>
)

data class MangaCard(
    val title: String,
    val slug: String,
    val url: String,
    val thumb: String,
    val latestChapter: String = "",
    val type: String = "",
    val score: String = ""
)

data class MangaDetail(
    val title: String,
    val slug: String,
    val thumb: String,
    val synopsis: String,
    val status: String,
    val type: String,
    val author: String,
    val genres: List<String>,
    val chapters: List<MangaChapter>
)

data class MangaChapter(
    val title: String,
    val slug: String,
    val url: String,
    val date: String = ""
)

data class HistoryItem(
    val animeSlug: String,
    val animeTitle: String,
    val animeThumb: String,
    val lastEpSlug: String,
    val lastEpTitle: String,
    val lastEpIndex: Int,
    val positionMs: Long,
    val durationMs: Long,
    val progress: Float,
    val updatedAt: Long,
    val source: AnimeSource = AnimeSource.OTAKUDESU
)
