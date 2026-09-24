package com.zamnimeku.app.data.api

import com.zamnimeku.app.data.model.AnimeCard
import com.zamnimeku.app.data.model.AnimeDetail
import com.zamnimeku.app.data.model.Episode
import com.zamnimeku.app.data.model.Genre
import com.zamnimeku.app.data.model.ScheduleDay
import com.zamnimeku.app.data.model.VideoSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.net.URI
import java.net.URLEncoder
import java.util.Calendar
import java.util.concurrent.TimeUnit

object AnimeApi {
    const val BASE_URL = "https://www.mynimeku.com"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    private const val SERIES_TYPES = "BD,LA,MOVIE,MUSIC,ONA,OVA,SPECIAL,TV"
    private val QUALITY_ORDER = listOf("360p", "480p", "720p", "1080p")
    private val QUALITY_PATTERN = Regex("""(360|480|720|1080)\s*P""", RegexOption.IGNORE_CASE)

    private data class PlayerCandidate(
        val server: String,
        val url: String,
        val rank: Int
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", USER_AGENT)
                .header("Accept-Language", "id-ID,id;q=0.9,en;q=0.8")
                .build()
            chain.proceed(request)
        }
        .build()

    private fun getHtml(url: String): String {
        return try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) "" else response.body?.string() ?: ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    private fun getJson(url: String): JSONArray {
        return try {
            val request = Request.Builder().url(url).build()
            val body = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) "" else response.body?.string() ?: ""
            }
            if (body.isEmpty()) JSONArray() else JSONArray(body)
        } catch (_: Exception) {
            JSONArray()
        }
    }

    private fun normalizeHttpUrl(rawUrl: String, baseUrl: String = "$BASE_URL/"): String? {
        val value = rawUrl.trim()
        if (value.isEmpty()) return null
        val base = baseUrl.toHttpUrlOrNull()
        val resolved = when {
            value.startsWith("//") -> base?.let { "${it.scheme}:$value" }
            base != null -> base.resolve(value)?.toString()
            else -> value
        } ?: return null
        return resolved.toHttpUrlOrNull()
            ?.takeIf { it.scheme == "http" || it.scheme == "https" }
            ?.toString()
    }

    private fun cleanSlug(url: String): String {
        val path = try {
            URI(url).path
        } catch (_: Exception) {
            url.substringBefore('?').substringBefore('#')
        }
        return path.trimEnd('/').substringAfterLast('/')
    }

    private fun cleanField(value: String): String {
        val clean = value.trim()
        return if (
            clean.isEmpty() ||
            clean == "?" ||
            clean.contains("Unknown", ignoreCase = true)
        ) "" else clean
    }

    private fun pathSegment(value: String): String {
        return URLEncoder.encode(value.trim(), "UTF-8").replace("+", "%20")
    }

    private fun searchVariants(keyword: String): List<String> {
        val clean = keyword.trim()
        val variants = mutableListOf(clean)
        if (Regex("""re\s*:?\s*zero""", RegexOption.IGNORE_CASE).containsMatchIn(clean)) {
            variants.add("Re Zero")
        }
        return variants.filter { it.isNotEmpty() }.distinct()
    }

    private fun imageUrl(element: Element): String {
        val image = element.selectFirst("img") ?: return ""
        val candidates = listOf(
            image.attr("data-lazy-src"),
            image.attr("data-src"),
            image.attr("src")
        )
        for (candidate in candidates) {
            if (candidate.isNotBlank() && !candidate.contains("data:image/svg+xml", ignoreCase = true)) {
                normalizeHttpUrl(candidate)?.let { return it }
            }
        }
        return ""
    }

    private fun parseCatalogCards(document: org.jsoup.nodes.Document): List<AnimeCard> {
        return document.select(".mynimeku-mix-feed__item").mapNotNull { item ->
            val link = item.selectFirst("a[href*=\"/series/\"]")?.attr("href")?.trim().orEmpty()
            val title = item.selectFirst(".mynimeku-mix-feed__series-title")?.text()?.trim().orEmpty()
            if (link.isEmpty() || title.isEmpty()) return@mapNotNull null
            val type = cleanField(item.selectFirst(".mynimeku-mix-feed__type")?.text().orEmpty())
            val status = cleanField(item.selectFirst(".mynimeku-mix-feed__status")?.text().orEmpty())
            AnimeCard(
                title = title,
                slug = cleanSlug(link),
                url = link,
                thumb = imageUrl(item),
                episode = type,
                day = status
            )
        }
    }

    private fun parseSearchCards(document: org.jsoup.nodes.Document): List<AnimeCard> {
        return document.select(".mynimeku-search-feed__item").mapNotNull { item ->
            val link = item.selectFirst("a[href*=\"/series/\"]")?.attr("href")?.trim().orEmpty()
            val title = item.selectFirst(".mynimeku-search-feed__series-title")?.text()?.trim().orEmpty()
            if (link.isEmpty() || title.isEmpty()) return@mapNotNull null
            AnimeCard(
                title = title,
                slug = cleanSlug(link),
                url = link,
                thumb = imageUrl(item),
                episode = cleanField(item.selectFirst(".mynimeku-search-feed__type")?.text().orEmpty()),
                day = cleanField(item.selectFirst(".mynimeku-search-feed__status")?.text().orEmpty())
            )
        }
    }

    private fun parseRestSearch(items: JSONArray): List<AnimeCard> {
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val link = item.optString("link").trim()
                val title = Jsoup.parse(item.optJSONObject("title")?.optString("rendered").orEmpty()).text()
                if (link.isEmpty() || title.isEmpty()) continue
                val media = item.optJSONObject("_embedded")
                    ?.optJSONArray("wp:featuredmedia")
                    ?.optJSONObject(0)
                val thumb = media?.optString("source_url").orEmpty()
                add(
                    AnimeCard(
                        title = title,
                        slug = cleanSlug(link),
                        url = link,
                        thumb = normalizeHttpUrl(thumb).orEmpty()
                    )
                )
            }
        }
    }

    private fun parseLatestCards(document: org.jsoup.nodes.Document): List<AnimeCard> {
        return document.select(".mynimeku-update-feed__item").mapNotNull { item ->
            val link = item.selectFirst("a[href*=\"/series/\"]")?.attr("href")?.trim().orEmpty()
            val title = item.selectFirst(".mynimeku-update-feed__series-title")?.text()?.trim().orEmpty()
            if (link.isEmpty() || title.isEmpty()) return@mapNotNull null
            val badges = item.select(".mynimeku-update-feed__badge").map { cleanField(it.text()) }
            val latest = cleanField(item.selectFirst(".mynimeku-update-feed__latest-pill")?.text().orEmpty())
            AnimeCard(
                title = title,
                slug = cleanSlug(link),
                url = link,
                thumb = imageUrl(item),
                episode = latest,
                day = badges.getOrNull(1).orEmpty(),
                date = cleanField(item.selectFirst(".mynimeku-update-feed__date")?.text().orEmpty())
            )
        }
    }

    private suspend fun getCatalog(path: String, page: Int): List<AnimeCard> = withContext(Dispatchers.IO) {
        val pagePath = if (page > 1) "$path/page/$page/" else "$path"
        val html = getHtml("$BASE_URL$pagePath")
        if (html.isEmpty()) return@withContext emptyList()
        parseCatalogCards(Jsoup.parse(html))
    }

    suspend fun getOngoingAnime(page: Int = 1): List<AnimeCard> {
        return getCatalog("/full-list/mix/s:on-going~t:$SERIES_TYPES/", page)
    }

    suspend fun getCompleteAnime(page: Int = 1): List<AnimeCard> {
        return getCatalog("/full-list/mix/s:completed~t:$SERIES_TYPES/", page)
    }

    suspend fun searchAnime(keyword: String): List<AnimeCard> = withContext(Dispatchers.IO) {
        val variants = searchVariants(keyword)
        for (variant in variants) {
            val html = getHtml("$BASE_URL/search/${pathSegment(variant)}/")
            if (html.isNotEmpty()) {
                val cards = parseSearchCards(Jsoup.parse(html))
                if (cards.isNotEmpty()) return@withContext cards
            }
        }
        for (variant in variants) {
            val cards = parseRestSearch(
                getJson("$BASE_URL/wp-json/wp/v2/series?per_page=24&search=${pathSegment(variant)}&_embed=1")
            )
            if (cards.isNotEmpty()) return@withContext cards
        }
        emptyList()
    }

    suspend fun getGenreList(): List<Genre> = withContext(Dispatchers.IO) {
        val html = getHtml("$BASE_URL/genre-list/")
        if (html.isEmpty()) return@withContext emptyList()
        val document = Jsoup.parse(html)
        document.select("a[href*=\"/genre/\"]").mapNotNull { link ->
            val url = link.attr("href").trim()
            val name = link.text().trim()
            if (url.isEmpty() || name.isEmpty()) return@mapNotNull null
            Genre(name = name, slug = cleanSlug(url), url = url)
        }.distinctBy { it.slug }
    }

    suspend fun getAnimeByGenre(genreSlug: String, page: Int = 1): List<AnimeCard> = withContext(Dispatchers.IO) {
        val pagePath = if (page > 1) "/genre/$genreSlug/page/$page/" else "/genre/$genreSlug/"
        val html = getHtml("$BASE_URL$pagePath")
        if (html.isEmpty()) return@withContext emptyList()
        Jsoup.parse(html).select(".mynimeku-taxmix-feed__item").mapNotNull { item ->
            val link = item.selectFirst("a[href*=\"/series/\"]")?.attr("href")?.trim().orEmpty()
            val title = item.selectFirst(".mynimeku-taxmix-feed__series-title")?.text()?.trim().orEmpty()
            if (link.isEmpty() || title.isEmpty()) return@mapNotNull null
            AnimeCard(
                title = title,
                slug = cleanSlug(link),
                url = link,
                thumb = imageUrl(item),
                episode = cleanField(item.selectFirst(".mynimeku-taxmix-feed__type")?.text().orEmpty()),
                day = cleanField(item.selectFirst(".mynimeku-taxmix-feed__status")?.text().orEmpty())
            )
        }
    }

    suspend fun getSchedule(): List<ScheduleDay> = withContext(Dispatchers.IO) {
        val html = getHtml("$BASE_URL/latest-series/")
        if (html.isEmpty()) return@withContext emptyList()
        val cards = parseLatestCards(Jsoup.parse(html))
        if (cards.isEmpty()) return@withContext emptyList()
        val dayNames = listOf("Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")
        val today = dayNames[(Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)]
        listOf(ScheduleDay(day = today, animes = cards))
    }

    suspend fun getAnimeDetail(animeSlug: String): AnimeDetail? = withContext(Dispatchers.IO) {
        val slug = animeSlug.trim().trim('/')
        val html = getHtml("$BASE_URL/series/$slug/")
        if (html.isEmpty()) return@withContext null
        val document = Jsoup.parse(html)
        val title = document.selectFirst(".komik-series-hero__title")?.text()?.trim().orEmpty()
        if (title.isEmpty()) return@withContext null
        val cover = document.selectFirst(".komik-series-hero__cover img")?.let { imageUrl(it) }.orEmpty()
        val synopsis = document.selectFirst(".komik-series-hero__synopsis .komik-series-entry")
            ?.text()?.trim().orEmpty()

        fun info(vararg labels: String): String {
            val normalized = labels.map { it.lowercase() }
            val row = document.select(".komik-series-info .komik-series-table > tbody > tr")
                .firstOrNull { element ->
                    val key = element.selectFirst("th")?.text()?.trim()?.lowercase().orEmpty()
                    normalized.any { key.contains(it) }
                }
            return cleanField(row?.selectFirst("td")?.text().orEmpty())
        }

        val rating = info("Rating")
        val score = Regex("""\d+(?:[.,]\d+)?""").find(rating)?.value ?: "-"
        val status = info("Status").ifEmpty { "On-Going" }
        val type = info("Type").ifEmpty { "TV" }
        val episodes = document.select(".komik-series-chapter-item[href*=\"/episode/\"]").map { element ->
            val link = element.attr("href").trim()
            Episode(
                title = element.selectFirst(".komik-series-chapter-item__title")?.text()?.trim().orEmpty(),
                slug = cleanSlug(link),
                url = link,
                date = cleanField(element.selectFirst(".komik-series-chapter-item__date")?.text().orEmpty())
            )
        }.filter { it.slug.isNotEmpty() }

        AnimeDetail(
            title = title,
            slug = slug,
            thumb = cover,
            synopsis = synopsis,
            score = score,
            producer = info("Producer", "Licensor").ifEmpty { "-" },
            type = type,
            status = status,
            totalEpisodes = info("Episodes").ifEmpty { episodes.size.takeIf { value -> value > 0 }?.toString() ?: "?" },
            duration = info("Duration").ifEmpty { "-" },
            releaseDate = info("Release Date", "Aired").ifEmpty { "-" },
            studio = info("Studio").ifEmpty { "-" },
            genres = document.select(".komik-series-taxonomy__terms a").map { it.text().trim() }.filter { it.isNotEmpty() },
            episodes = episodes
        )
    }

    private fun playerRank(provider: String, type: String): Int {
        return when {
            provider.equals("DRIVE", ignoreCase = true) -> 0
            provider.equals("PROXY", ignoreCase = true) -> 1
            provider.equals("CLOUD", ignoreCase = true) && type.equals("private", ignoreCase = true) -> 2
            else -> 3
        }
    }

    suspend fun getVideoCandidates(episodeSlug: String): List<VideoSource> = withContext(Dispatchers.IO) {
        val html = getHtml("$BASE_URL/episode/${pathSegment(episodeSlug)}/")
        if (html.isEmpty()) return@withContext emptyList()
        val document = Jsoup.parse(html)
        val grouped = linkedMapOf<String, MutableList<PlayerCandidate>>()

        for (button in document.select(".mynimeku-episode-server-btn[data-player-url][data-player-host]")) {
            val rawUrl = button.attr("data-player-url").trim()
            val host = button.attr("data-player-host").trim()
            val type = button.attr("data-player-type").trim().ifEmpty { "public" }
            val qualityMatch = QUALITY_PATTERN.find(host)
            val quality = qualityMatch?.let { "${it.groupValues[1]}p" } ?: "Auto"
            val provider = host.substringBefore(' ').trim().uppercase()
            val url = normalizeHttpUrl(rawUrl) ?: continue
            grouped.getOrPut(quality) { mutableListOf() }.add(
                PlayerCandidate(
                    server = "${type.uppercase()} $host",
                    url = url,
                    rank = playerRank(provider, type)
                )
            )
        }

        val qualities = QUALITY_ORDER.filter { grouped.containsKey(it) } +
            if (grouped.containsKey("Auto")) listOf("Auto") else emptyList()
        qualities.map { quality ->
            val candidates = grouped.getValue(quality)
                .distinctBy { it.url }
                .sortedBy { it.rank }
            VideoSource(
                quality = quality,
                server = candidates.first().server,
                url = candidates.first().url,
                backupUrls = candidates.drop(1).map { it.url }
            )
        }
    }
}
