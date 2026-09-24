package com.zamnimeku.app.data.api

import android.util.Base64
import com.zamnimeku.app.data.model.*
import kotlinx.coroutines.*
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object OtakuApi {
    const val BASE_URL = "https://otakudesu.blog"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    private const val AUTO_QUALITY = "Auto"
    private val QUALITY_ORDER = listOf("360p", "480p", "720p", "1080p")
    private val UNICODE_ESCAPE = Regex("""\\u([0-9a-fA-F]{4})""")
    private val QUOTED_MEDIA_URL = Regex("""["']([^"']+?\.(?:m3u8|mp4)(?:[?#][^"']*)?)["']""", RegexOption.IGNORE_CASE)
    private val MEDIA_EXTENSION = Regex("""\.(?:m3u8|mp4)(?:[?#]|$)""", RegexOption.IGNORE_CASE)
    private val URL_QUALITY = Regex("""(?:^|[^0-9])(360|480|720|1080)(?:p)?(?=$|[^0-9])""", RegexOption.IGNORE_CASE)

    private data class MirrorRequest(
        val quality: String,
        val server: String,
        val dataContent: String
    )

    private data class ResolvedMirror(
        val quality: String,
        val server: String,
        val url: String
    )

    private data class StreamCandidate(
        val server: String,
        val url: String
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("User-Agent", USER_AGENT)
                .header("Referer", "$BASE_URL/")
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

    private fun cleanSlug(url: String): String {
        val clean = url.trimEnd('/')
        return clean.substringAfterLast('/')
    }

    // Situs menulis "Unknown"/"?" untuk data kosong (episode, rating, dst),
    // kadang dalam bentuk "Eps Unknown". Ubah jadi string kosong supaya
    // UI tidak menampilkan unknown.
    private fun cleanField(s: String): String {
        val t = s.trim()
        return if (t.isEmpty() || t == "?" || t.contains("Unknown", ignoreCase = true)) "" else t
    }

    suspend fun getOngoingAnime(page: Int = 1): List<AnimeCard> = withContext(Dispatchers.IO) {
        val path = if (page == 1) "$BASE_URL/ongoing-anime/" else "$BASE_URL/ongoing-anime/page/$page/"
        val html = getHtml(path)
        if (html.isEmpty()) return@withContext emptyList()

        val doc = Jsoup.parse(html)
        val items = doc.select("div.detpost")
        val result = mutableListOf<AnimeCard>()

        for (item in items) {
            val link = item.selectFirst("a[href*=/anime/]")?.attr("href") ?: continue
            val title = item.selectFirst("h2.jdlflm")?.text()?.trim() ?: continue
            val thumb = item.selectFirst("img")?.attr("src") ?: ""
            val ep = cleanField(item.selectFirst("div.epz")?.text() ?: "")
            val day = cleanField(item.selectFirst("div.epztipe")?.text() ?: "")
            val date = cleanField(item.selectFirst("div.newnime")?.text() ?: "")

            result.add(
                AnimeCard(
                    title = title,
                    slug = cleanSlug(link),
                    url = link,
                    thumb = thumb,
                    episode = ep,
                    day = day,
                    date = date
                )
            )
        }
        result
    }

    suspend fun getCompleteAnime(page: Int = 1): List<AnimeCard> = withContext(Dispatchers.IO) {
        val path = if (page == 1) "$BASE_URL/complete-anime/" else "$BASE_URL/complete-anime/page/$page/"
        val html = getHtml(path)
        if (html.isEmpty()) return@withContext emptyList()

        val doc = Jsoup.parse(html)
        val items = doc.select("div.detpost")
        val result = mutableListOf<AnimeCard>()

        for (item in items) {
            val link = item.selectFirst("a[href*=/anime/]")?.attr("href") ?: continue
            val title = item.selectFirst("h2.jdlflm")?.text()?.trim() ?: continue
            val thumb = item.selectFirst("img")?.attr("src") ?: ""
            val ep = cleanField(item.selectFirst("div.epz")?.text() ?: "")
            val score = cleanField(item.selectFirst("div.epztipe")?.text() ?: "")
            val date = cleanField(item.selectFirst("div.newnime")?.text() ?: "")

            result.add(
                AnimeCard(
                    title = title,
                    slug = cleanSlug(link),
                    url = link,
                    thumb = thumb,
                    episode = ep,
                    score = score,
                    date = date
                )
            )
        }
        result
    }

    suspend fun searchAnime(keyword: String): List<AnimeCard> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/?s=${java.net.URLEncoder.encode(keyword, "UTF-8")}&post_type=anime"
        val html = getHtml(url)
        if (html.isEmpty()) return@withContext emptyList()

        val doc = Jsoup.parse(html)
        val items = doc.select("ul.chivsrc li")
        val result = mutableListOf<AnimeCard>()

        for (item in items) {
            val link = item.selectFirst("h2 a")?.attr("href") ?: continue
            val title = item.selectFirst("h2 a")?.text()?.trim() ?: continue
            val thumb = item.selectFirst("img")?.attr("src") ?: ""
            val status = cleanField(item.selectFirst("div.set:contains(Status)")?.text()?.replace("Status :", "") ?: "")
            val score = cleanField(item.selectFirst("div.set:contains(Rating)")?.text()?.replace("Rating :", "") ?: "")

            result.add(
                AnimeCard(
                    title = title,
                    slug = cleanSlug(link),
                    url = link,
                    thumb = thumb,
                    episode = status,
                    score = score
                )
            )
        }
        result
    }

    suspend fun getGenreList(): List<Genre> = withContext(Dispatchers.IO) {
        val html = getHtml("$BASE_URL/genre-list/")
        if (html.isEmpty()) return@withContext emptyList()

        val doc = Jsoup.parse(html)
        val items = doc.select("ul.genres li a")
        val result = mutableListOf<Genre>()

        for (item in items) {
            val link = item.attr("href")
            val name = item.text().trim()
            if (link.isNotEmpty() && name.isNotEmpty()) {
                result.add(Genre(name = name, slug = cleanSlug(link), url = link))
            }
        }
        result
    }

    suspend fun getAnimeByGenre(genreSlug: String, page: Int = 1): List<AnimeCard> = withContext(Dispatchers.IO) {
        val path = if (page == 1) "$BASE_URL/genres/$genreSlug/" else "$BASE_URL/genres/$genreSlug/page/$page/"
        val html = getHtml(path)
        if (html.isEmpty()) return@withContext emptyList()

        val doc = Jsoup.parse(html)
        val items = doc.select("div.col-anime")
        val result = mutableListOf<AnimeCard>()

        for (item in items) {
            val link = item.selectFirst("div.col-anime-title a")?.attr("href") ?: continue
            val title = item.selectFirst("div.col-anime-title a")?.text()?.trim() ?: continue
            val thumb = item.selectFirst("div.col-anime-cover img")?.attr("src") ?: ""
            val ep = cleanField(item.selectFirst("div.col-anime-eps")?.text() ?: "")
            val score = cleanField(item.selectFirst("div.col-anime-rating")?.text() ?: "")

            result.add(
                AnimeCard(
                    title = title,
                    slug = cleanSlug(link),
                    url = link,
                    thumb = thumb,
                    episode = ep,
                    score = score
                )
            )
        }
        result
    }

    suspend fun getSchedule(): List<ScheduleDay> = withContext(Dispatchers.IO) {
        val html = getHtml("$BASE_URL/jadwal-rilis/")
        if (html.isEmpty()) return@withContext emptyList()

        val doc = Jsoup.parse(html)
        val kg = doc.select("div.kglist321")
        val result = mutableListOf<ScheduleDay>()

        for (daySec in kg) {
            val dayName = daySec.selectFirst("h2")?.text()?.trim() ?: continue
            val links = daySec.select("ul li a")
            val animeList = mutableListOf<AnimeCard>()

            for (a in links) {
                val href = a.attr("href")
                val title = a.text().trim()
                if (href.isNotEmpty() && title.isNotEmpty()) {
                    animeList.add(
                        AnimeCard(
                            title = title,
                            slug = cleanSlug(href),
                            url = href,
                            thumb = "",
                            day = dayName
                        )
                    )
                }
            }
            if (animeList.isNotEmpty()) {
                result.add(ScheduleDay(day = dayName, animes = animeList))
            }
        }
        result
    }

    suspend fun getAnimeDetail(animeSlug: String): AnimeDetail? = withContext(Dispatchers.IO) {
        val html = getHtml("$BASE_URL/anime/$animeSlug/")
        if (html.isEmpty()) return@withContext null

        val doc = Jsoup.parse(html)
        val title = doc.selectFirst("div.infozingle p:contains(Judul)")?.text()?.replace("Judul:", "")?.trim()
            ?: doc.selectFirst("div.jdlarea h1")?.text()?.trim() ?: animeSlug
        val thumb = doc.selectFirst("div.fotoanime img")?.attr("src") ?: ""
        val synopsis = doc.select("div.sinopc p").text().trim()
        val score = cleanField(doc.selectFirst("div.infozingle p:contains(Skor)")?.text()?.replace("Skor:", "") ?: "").ifEmpty { "-" }
        val producer = cleanField(doc.selectFirst("div.infozingle p:contains(Produser)")?.text()?.replace("Produser:", "") ?: "").ifEmpty { "-" }
        val type = cleanField(doc.selectFirst("div.infozingle p:contains(Tipe)")?.text()?.replace("Tipe:", "") ?: "").ifEmpty { "Anime" }
        val status = cleanField(doc.selectFirst("div.infozingle p:contains(Status)")?.text()?.replace("Status:", "") ?: "").ifEmpty { "Ongoing" }
        val rawTotalEp = doc.selectFirst("div.infozingle p:contains(Total Episode)")?.text()?.replace("Total Episode:", "")?.trim() ?: "?"
        val duration = cleanField(doc.selectFirst("div.infozingle p:contains(Durasi)")?.text()?.replace("Durasi:", "") ?: "").ifEmpty { "24 min" }
        val releaseDate = cleanField(doc.selectFirst("div.infozingle p:contains(Tanggal Rilis)")?.text()?.replace("Tanggal Rilis:", "") ?: "").ifEmpty { "-" }
        val studio = cleanField(doc.selectFirst("div.infozingle p:contains(Studio)")?.text()?.replace("Studio:", "") ?: "").ifEmpty { "-" }

        val genreList = doc.select("div.infozingle p:contains(Genre) a").map { it.text().trim() }

        val episodes = mutableListOf<Episode>()
        val epElements = doc.select("div.episodelist ul li")
        for (epEl in epElements) {
            val a = epEl.selectFirst("span a") ?: continue
            val link = a.attr("href")
            val epTitle = a.text().trim()
            val date = epEl.selectFirst("span.zeebr")?.text()?.trim() ?: ""
            if (link.contains("/episode/")) {
                episodes.add(
                    Episode(
                        title = epTitle,
                        slug = cleanSlug(link),
                        url = link,
                        date = date
                    )
                )
            }
        }

        // Situs menulis "Unknown" untuk ongoing — ganti dengan jumlah
        // episode yang sudah rilis supaya tidak tampil unknown.
        val totalEp = when {
            rawTotalEp.isBlank() || rawTotalEp == "?" || rawTotalEp.contains("Unknown", ignoreCase = true) ->
                if (episodes.isNotEmpty()) episodes.size.toString() else rawTotalEp.ifEmpty { "?" }
            else -> rawTotalEp
        }

        AnimeDetail(
            title = title,
            slug = animeSlug,
            thumb = thumb,
            synopsis = synopsis,
            score = score,
            producer = producer,
            type = type,
            status = status,
            totalEpisodes = totalEp,
            duration = duration,
            releaseDate = releaseDate,
            studio = studio,
            genres = genreList,
            episodes = episodes
        )
    }

    // ── DEAN-EDWARDS UNPACKER (Base62) ──
    private fun unbase(str: String, radix: Int): Int {
        if (radix <= 36) {
            return str.toIntOrNull(radix) ?: -1
        }
        val chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        var result = 0
        for (i in str.indices) {
            val idx = chars.indexOf(str[i])
            if (idx < 0 || idx >= radix) return -1
            result = result * radix + idx
        }
        return result
    }

    private fun unpackPacker(src: String): String {
        val pattern = Pattern.compile("""\}\('([\s\S]*?)',(\d+),(\d+),'([\s\S]*?)'\.split\('\|'\)""")
        val matcher = pattern.matcher(src)
        if (!matcher.find()) return src

        var p = matcher.group(1)!!.replace("\\'", "'")
        val a = matcher.group(2)!!.toIntOrNull() ?: 10
        val k = matcher.group(4)!!.split("|")

        val wordPattern = Pattern.compile("\\b\\w+\\b")
        val wordMatcher = wordPattern.matcher(p)
        val sb = StringBuffer()

        while (wordMatcher.find()) {
            val token = wordMatcher.group()
            val idx = unbase(token, a)
            if (idx in 0 until k.size && k[idx].isNotEmpty()) {
                wordMatcher.appendReplacement(sb, MatcherQuote(k[idx]))
            } else {
                wordMatcher.appendReplacement(sb, MatcherQuote(token))
            }
        }
        wordMatcher.appendTail(sb)
        return sb.toString()
    }

    private fun MatcherQuote(s: String): String {
        return s.replace("\\", "\\\\").replace("$", "\\$")
    }

    private fun decodeUrlText(value: String): String {
        val slashDecoded = value.replace("\\/", "/")
        val unicodeDecoded = UNICODE_ESCAPE.replace(slashDecoded) { match ->
            match.groupValues[1].toInt(16).toChar().toString()
        }
        return Parser.unescapeEntities(unicodeDecoded, true)
    }

    private fun resolveHttpUrl(rawUrl: String, baseUrl: String): String? {
        var value = decodeUrlText(rawUrl.trim())
        if (value.length >= 2 && ((value.first() == '"' && value.last() == '"') ||
                    (value.first() == '\'' && value.last() == '\''))
        ) {
            value = value.substring(1, value.length - 1)
        }
        val base = baseUrl.toHttpUrlOrNull()
        val resolved = when {
            value.startsWith("//") -> base?.let { "${it.scheme}:$value" }
            base != null -> base.resolve(value)?.toString()
            else -> value
        } ?: return null
        val url = resolved.toHttpUrlOrNull() ?: return null
        return url.toString().takeIf { url.scheme == "http" || url.scheme == "https" }
    }

    private fun declaredQuality(rawQuality: String): String? {
        val value = rawQuality.lowercase()
        return when {
            value.contains("1080") -> "1080p"
            value.contains("720") -> "720p"
            value.contains("480") -> "480p"
            value.contains("360") -> "360p"
            else -> null
        }
    }

    private fun qualityFromUrl(url: String): String? {
        val match = URL_QUALITY.find(decodeUrlText(url)) ?: return null
        return "${match.groupValues[1]}p"
    }

    private fun isAbsoluteUrlReference(value: String): Boolean {
        return value.startsWith("http://", ignoreCase = true) ||
            value.startsWith("https://", ignoreCase = true) ||
            value.startsWith("//")
    }

    private fun extractKeyedUrl(text: String, baseUrl: String): String? {
        val mediaKeyPattern = Regex(
            """["']?\b(videoURL|file)\b["']?\s*[:=]\s*["']([^"']+)["']""",
            RegexOption.IGNORE_CASE
        )
        val mediaKeys = mediaKeyPattern.findAll(text).mapNotNull { match ->
            val key = match.groupValues[1]
            val rawUrl = match.groupValues[2]
            resolveHttpUrl(rawUrl, baseUrl)?.let { Triple(key, rawUrl, it) }
        }.toList()

        mediaKeys.firstOrNull { (_, _, url) -> MEDIA_EXTENSION.containsMatchIn(url) }
            ?.let { return it.third }
        mediaKeys.firstOrNull { (key, rawUrl, _) ->
            key.equals("videoURL", ignoreCase = true) && isAbsoluteUrlReference(rawUrl)
        }?.let { return it.third }

        val hlsMatch = Regex(
            """["']?\bhls\b["']?\s*[:=]\s*["']([^"']+)["']""",
            RegexOption.IGNORE_CASE
        ).find(text)
        if (hlsMatch != null) {
            val rawUrl = hlsMatch.groupValues[1]
            resolveHttpUrl(rawUrl, baseUrl)
                ?.takeIf {
                    MEDIA_EXTENSION.containsMatchIn(it) ||
                        isAbsoluteUrlReference(rawUrl) ||
                        rawUrl.startsWith("/")
                }
                ?.let { return it }
        }

        mediaKeys.firstOrNull { (_, rawUrl, _) -> isAbsoluteUrlReference(rawUrl) }
            ?.let { return it.third }

        val sourceMatch = Regex(
            """["']?\bsrc\b["']?\s*[:=]\s*["']([^"']+)["']""",
            RegexOption.IGNORE_CASE
        ).find(text) ?: return null
        return resolveHttpUrl(sourceMatch.groupValues[1], baseUrl)
            ?.takeIf { MEDIA_EXTENSION.containsMatchIn(it) }
    }

    private fun extractQuotedMediaUrl(text: String, baseUrl: String): String? {
        val match = QUOTED_MEDIA_URL.find(text) ?: return null
        return resolveHttpUrl(match.groupValues[1], baseUrl)
    }

    private fun extractVideoFromEmbed(embedUrl: String): String? {
        if (embedUrl.isEmpty() || embedUrl.contains("mega.nz")) return null
        return try {
            val request = Request.Builder().url(embedUrl).build()
            val html = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.string() ?: ""
            }
            if (html.isEmpty()) return null

            val decodedHtml = decodeUrlText(html)
            val document = Jsoup.parse(decodedHtml)
            for (element in document.select("source[src], video[src]")) {
                resolveHttpUrl(element.attr("src"), embedUrl)
                    ?.takeIf { MEDIA_EXTENSION.containsMatchIn(it) }
                    ?.let { return it }
            }
            extractKeyedUrl(decodedHtml, embedUrl)?.let { return it }

            val unpacked = unpackPacker(decodedHtml)
            extractKeyedUrl(unpacked, embedUrl)?.let { return it }
            extractQuotedMediaUrl(unpacked, embedUrl)?.let { return it }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun scoreStreamUrl(url: String): Int {
        val lowerUrl = url.lowercase()
        if (lowerUrl.contains("odcloud.net") || lowerUrl.contains("desustream.net") || lowerUrl.contains("desustream.me")) return 100
        if (lowerUrl.contains("archive.org")) return 95
        if (lowerUrl.contains(".mp4")) return 80
        if (lowerUrl.contains(".m3u8")) return 70
        if (lowerUrl.contains("dramiyos-cdn") || lowerUrl.contains("vidhide")) return 60
        return 10
    }

    private fun resolveMirror(ajaxUrl: String, nonce: String, mirror: MirrorRequest): ResolvedMirror? {
        return try {
            var normalizedData = mirror.dataContent.trim()
            while (normalizedData.length % 4 != 0) normalizedData += "="
            val metadata = JSONObject(String(Base64.decode(normalizedData, Base64.DEFAULT), Charsets.UTF_8))

            val postRequest = Request.Builder()
                .url(ajaxUrl)
                .post(
                    FormBody.Builder()
                        .add("id", metadata.optString("id"))
                        .add("i", metadata.optString("i"))
                        .add("q", metadata.optString("q"))
                        .add("nonce", nonce)
                        .add("action", "2a3505c93b0035d3f455df82bf976b84")
                        .build()
                )
                .header("X-Requested-With", "XMLHttpRequest")
                .build()

            val responseJson = client.newCall(postRequest).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.string() ?: ""
            }
            val base64Data = JSONObject(responseJson).optString("data")
            if (base64Data.isEmpty()) return null

            var normalizedResponse = base64Data.trim()
            while (normalizedResponse.length % 4 != 0) normalizedResponse += "="
            val iframeHtml = decodeUrlText(
                String(Base64.decode(normalizedResponse, Base64.DEFAULT), Charsets.UTF_8)
            )
            val iframeDocument = Jsoup.parse(iframeHtml)
            val iframeSource = iframeDocument.selectFirst("iframe[src], source[src]")?.attr("src")
                ?: Regex("""\bsrc\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                    .find(iframeHtml)?.groupValues?.get(1)
                ?: return null
            val embedUrl = resolveHttpUrl(iframeSource, "$BASE_URL/") ?: return null
            val videoUrl = extractVideoFromEmbed(embedUrl) ?: return null
            val quality = declaredQuality(metadata.optString("q"))
                ?: declaredQuality(mirror.quality)
                ?: ""

            ResolvedMirror(
                quality = quality,
                server = mirror.server,
                url = videoUrl
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getVideoCandidates(episodeSlug: String): List<VideoSource> = withContext(Dispatchers.IO) {
        val qualityMap = linkedMapOf<String, MutableList<StreamCandidate>>()

        fun addStream(declaredQuality: String, serverName: String, rawUrl: String) {
            val url = resolveHttpUrl(rawUrl, "$BASE_URL/") ?: return
            if (url.contains("googlevideo.com") && url.contains("&ip=")) return
            val quality = qualityFromUrl(url)
                ?: declaredQuality(declaredQuality)
                ?: AUTO_QUALITY
            val host = url.toHttpUrlOrNull()?.host ?: "Server"
            val server = serverName.trim().ifEmpty { host }
            val candidates = qualityMap.getOrPut(quality) { mutableListOf() }
            if (candidates.none { it.url == url }) {
                candidates.add(StreamCandidate(server = server, url = url))
            }
        }

        try {
            val episodeHtml = getHtml("$BASE_URL/episode/$episodeSlug/")
            if (episodeHtml.isNotEmpty()) {
                val document = Jsoup.parse(episodeHtml)
                val defaultIframe = document.selectFirst("iframe")?.attr("src").orEmpty()
                val defaultIframeUrl = resolveHttpUrl(defaultIframe, "$BASE_URL/")
                if (defaultIframeUrl != null) {
                    extractVideoFromEmbed(defaultIframeUrl)?.let { videoUrl ->
                        addStream("", defaultIframeUrl.toHttpUrlOrNull()?.host.orEmpty(), videoUrl)
                    }
                }

                val mirrors = document.select("ul[class] li a[data-content]").mapNotNull { anchor ->
                    val qualityClass = anchor.closest("ul[class]")?.classNames()
                        ?.firstOrNull { declaredQuality(it) != null }
                        ?: return@mapNotNull null
                    MirrorRequest(
                        quality = qualityClass,
                        server = anchor.text().trim(),
                        dataContent = anchor.attr("data-content")
                    )
                }

                val ajaxUrl = "$BASE_URL/wp-admin/admin-ajax.php"
                val nonceRequest = Request.Builder()
                    .url(ajaxUrl)
                    .post(FormBody.Builder().add("action", "aa1208d27f29ca340c92c66d1926f13f").build())
                    .header("X-Requested-With", "XMLHttpRequest")
                    .build()
                val nonce = client.newCall(nonceRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        ""
                    } else {
                        val responseJson = response.body?.string().orEmpty()
                        try {
                            JSONObject(responseJson).optString("data")
                        } catch (_: Exception) {
                            ""
                        }
                    }
                }

                if (nonce.isNotEmpty() && mirrors.isNotEmpty()) {
                    val resolvedMirrors = mirrors.map { mirror ->
                        async(Dispatchers.IO) {
                            withTimeoutOrNull(9000) {
                                resolveMirror(ajaxUrl, nonce, mirror)
                            }
                        }
                    }.awaitAll().filterNotNull()

                    resolvedMirrors.forEach { mirror ->
                        addStream(mirror.quality, mirror.server, mirror.url)
                    }
                }
            }
        } catch (_: Exception) {}

        val availableQualities = QUALITY_ORDER.filter { qualityMap.containsKey(it) }.toMutableList()
        if (qualityMap.containsKey(AUTO_QUALITY)) availableQualities.add(AUTO_QUALITY)

        return@withContext availableQualities.map { quality ->
            val sortedCandidates = qualityMap.getValue(quality).sortedByDescending { scoreStreamUrl(it.url) }
            VideoSource(
                quality = quality,
                server = sortedCandidates.first().server,
                url = sortedCandidates.first().url,
                backupUrls = sortedCandidates.drop(1).map { it.url }
            )
        }
    }
}
