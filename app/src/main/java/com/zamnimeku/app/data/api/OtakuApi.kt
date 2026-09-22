package com.zamnimeku.app.data.api

import android.util.Base64
import com.zamnimeku.app.data.model.*
import kotlinx.coroutines.*
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object OtakuApi {
    const val BASE_URL = "https://otakudesu.blog"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

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
            val ep = item.selectFirst("div.epz")?.text()?.trim() ?: ""
            val day = item.selectFirst("div.epztipe")?.text()?.trim() ?: ""
            val date = item.selectFirst("div.newnime")?.text()?.trim() ?: ""

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
            val ep = item.selectFirst("div.epz")?.text()?.trim() ?: ""
            val score = item.selectFirst("div.epztipe")?.text()?.trim() ?: ""
            val date = item.selectFirst("div.newnime")?.text()?.trim() ?: ""

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
            val status = item.selectFirst("div.set:contains(Status)")?.text()?.replace("Status :", "")?.trim() ?: ""
            val score = item.selectFirst("div.set:contains(Rating)")?.text()?.replace("Rating :", "")?.trim() ?: ""

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
            val ep = item.selectFirst("div.col-anime-eps")?.text()?.trim() ?: ""
            val score = item.selectFirst("div.col-anime-rating")?.text()?.trim() ?: ""

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
        val score = doc.selectFirst("div.infozingle p:contains(Skor)")?.text()?.replace("Skor:", "")?.trim() ?: "-"
        val producer = doc.selectFirst("div.infozingle p:contains(Produser)")?.text()?.replace("Produser:", "")?.trim() ?: "-"
        val type = doc.selectFirst("div.infozingle p:contains(Tipe)")?.text()?.replace("Tipe:", "")?.trim() ?: "Anime"
        val status = doc.selectFirst("div.infozingle p:contains(Status)")?.text()?.replace("Status:", "")?.trim() ?: "Ongoing"
        val rawTotalEp = doc.selectFirst("div.infozingle p:contains(Total Episode)")?.text()?.replace("Total Episode:", "")?.trim() ?: "?"
        val duration = doc.selectFirst("div.infozingle p:contains(Durasi)")?.text()?.replace("Durasi:", "")?.trim() ?: "24 min"
        val releaseDate = doc.selectFirst("div.infozingle p:contains(Tanggal Rilis)")?.text()?.replace("Tanggal Rilis:", "")?.trim() ?: "-"
        val studio = doc.selectFirst("div.infozingle p:contains(Studio)")?.text()?.replace("Studio:", "")?.trim() ?: "-"

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
            rawTotalEp.isBlank() || rawTotalEp == "?" || rawTotalEp.equals("Unknown", ignoreCase = true) ->
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

    private fun extractVideoFromEmbed(embedUrl: String): String? {
        if (embedUrl.isEmpty() || embedUrl.contains("mega.nz")) return null
        try {
            val request = Request.Builder().url(embedUrl).build()
            val html = client.newCall(request).execute().use { it.body?.string() ?: "" }
            if (html.isEmpty()) return null

            // 1. Tag source (ondesuhd, ondesu, otakuwatch)
            val sourceMatch = Pattern.compile("""<source[^>]+src=["']([^"']+)["']""").matcher(html)
            if (sourceMatch.find()) {
                val u = sourceMatch.group(1)!!
                if (!u.contains("googlevideo.com") || !u.contains("&ip=")) {
                    return u
                }
            }

            // 2. videoURL (odcdn, desustream)
            val vm = Pattern.compile("""videoURL\s*=\s*["']([^"']+)["']""").matcher(html)
            if (vm.find()) {
                return vm.group(1)
            }

            // 3. var vs = { file: "..." } (arcg / playerjs)
            val fm = Pattern.compile("""file\s*:\s*["']([^"']+)["']""").matcher(html)
            if (fm.find()) {
                val u = fm.group(1)!!
                if (u.startsWith("http")) return u
            }

            // 4. vidhide / filemoon (unpacked packer)
            if (html.contains("eval(function(p,a,c,k,e,d)")) {
                val code = unpackPacker(html)
                val m3u8Match = Pattern.compile("""["'](https?://[^"'\s]+\.m3u8[^"'\s]*)["']""").matcher(code)
                if (m3u8Match.find()) {
                    val m3u8 = m3u8Match.group(1)!!
                    if (!m3u8.startsWith("/dl?")) return m3u8
                }
                val mp4Match = Pattern.compile("""["'](https?://[^"'\s]+\.mp4[^"'\s]*)["']""").matcher(code)
                if (mp4Match.find()) {
                    val mp4 = mp4Match.group(1)!!
                    if (!mp4.startsWith("/dl?")) return mp4
                }
            }

            // 5. Generic direct mp4 / m3u8
            val p1 = Pattern.compile("""["'](https?://[^"'\s]+\.mp4[^"'\s]*)["']""").matcher(html)
            if (p1.find()) {
                val u = p1.group(1)!!
                if (!u.startsWith("/dl?") && (!u.contains("googlevideo.com") || !u.contains("&ip="))) {
                    return u
                }
            }
            val p2 = Pattern.compile("""["'](https?://[^"'\s]+\.m3u8[^"'\s]*)["']""").matcher(html)
            if (p2.find()) {
                val u = p2.group(1)!!
                if (!u.startsWith("/dl?") && (!u.contains("googlevideo.com") || !u.contains("&ip="))) {
                    return u
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun scoreStreamUrl(u: String): Int {
        val lu = u.lowercase()
        if (lu.contains("odcloud.net") || lu.contains("desustream.net") || lu.contains("desustream.me")) return 100
        if (lu.contains("archive.org")) return 95
        if (lu.contains(".mp4")) return 80
        if (lu.contains(".m3u8")) return 70
        if (lu.contains("dramiyos-cdn") || lu.contains("vidhide")) return 60
        return 10
    }

    suspend fun getVideoCandidates(episodeSlug: String): List<VideoSource> = withContext(Dispatchers.IO) {
        val qualityMap = mutableMapOf<String, MutableList<String>>(
            "360p" to mutableListOf(),
            "480p" to mutableListOf(),
            "720p" to mutableListOf(),
            "1080p" to mutableListOf()
        )

        fun addUrl(rawQ: String, url: String) {
            if (url.isEmpty()) return
            if (url.contains("googlevideo.com") && url.contains("&ip=")) return
            val q = rawQ.lowercase().trim()
            val targetQ = if (q.contains("1080")) {
                "1080p"
            } else if (q.contains("720")) {
                "720p"
            } else if (q.contains("480")) {
                "480p"
            } else {
                "360p"
            }
            if (!qualityMap[targetQ]!!.contains(url)) {
                qualityMap[targetQ]!!.add(url)
            }
        }

        try {
            val epHtml = getHtml("$BASE_URL/episode/$episodeSlug/")
            if (epHtml.isNotEmpty()) {
                val doc = Jsoup.parse(epHtml)

                // 1. Ambil Default Direct Stream (Sangat Cepat < 0.5 detik)
                val defaultIframe = doc.selectFirst("iframe")?.attr("src") ?: ""
                if (defaultIframe.isNotEmpty()) {
                    val directV = extractVideoFromEmbed(defaultIframe)
                    if (!directV.isNullOrEmpty()) {
                        addUrl("1080p", directV)
                        addUrl("720p", directV)
                        addUrl("480p", directV)
                        addUrl("360p", directV)
                    }
                }

                // 2. Parse mirror links
                val mirrorElements = doc.select("ul[class^=m] li a")
                val mirrors = mutableListOf<Triple<String, String, String>>()
                for (a in mirrorElements) {
                    val qClass = a.parent()?.parent()?.attr("class") ?: ""
                    val quality = qClass.replace("m", "")
                    val server = a.text().trim()
                    val data = a.attr("data-content")
                    if (data.isNotEmpty()) {
                        mirrors.add(Triple(quality, server, data))
                    }
                }

                // 3. Resolve mirrors via Ajax dengan batas waktu maksimal 4 detik
                val ajaxUrl = "$BASE_URL/wp-admin/admin-ajax.php"
                val nonceReq = Request.Builder()
                    .url(ajaxUrl)
                    .post(FormBody.Builder().add("action", "aa1208d27f29ca340c92c66d1926f13f").build())
                    .header("X-Requested-With", "XMLHttpRequest")
                    .build()

                try {
                    val nonceJson = client.newCall(nonceReq).execute().use { it.body?.string() ?: "" }
                    val nonce = try { JSONObject(nonceJson).optString("data") } catch (_: Exception) { "" }

                    if (nonce.isNotEmpty()) {
                        withTimeoutOrNull(4000) {
                            coroutineScope {
                                mirrors.map { (quality, _, dataContent) ->
                                    async(Dispatchers.IO) {
                                        try {
                                            var norm = dataContent.trim()
                                            while (norm.length % 4 != 0) norm += "="
                                            val decoded = String(Base64.decode(norm, Base64.DEFAULT))
                                            val meta = JSONObject(decoded)

                                            val postReq = Request.Builder()
                                                .url(ajaxUrl)
                                                .post(
                                                    FormBody.Builder()
                                                        .add("id", meta.optString("id"))
                                                        .add("i", meta.optString("i"))
                                                        .add("q", meta.optString("q"))
                                                        .add("nonce", nonce)
                                                        .add("action", "2a3505c93b0035d3f455df82bf976b84")
                                                        .build()
                                                )
                                                .header("X-Requested-With", "XMLHttpRequest")
                                                .build()

                                            val rJson = client.newCall(postReq).execute().use { it.body?.string() ?: "" }
                                            val b64Data = JSONObject(rJson).optString("data")
                                            if (b64Data.isNotEmpty()) {
                                                var b64Norm = b64Data.trim()
                                                while (b64Norm.length % 4 != 0) b64Norm += "="
                                                val iframeHtml = String(Base64.decode(b64Norm, Base64.DEFAULT))
                                                val iframeSrc = Pattern.compile("""src=["']([^"']+)["']""").matcher(iframeHtml)
                                                if (iframeSrc.find()) {
                                                    val embedUrl = iframeSrc.group(1)!!
                                                    val v = extractVideoFromEmbed(embedUrl)
                                                    if (!v.isNullOrEmpty()) {
                                                        addUrl(quality, v)
                                                    }
                                                }
                                            }
                                        } catch (_: Exception) {}
                                    }
                                }.awaitAll()
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}

        val allAvailable = mutableListOf<String>()
        qualityMap.values.forEach { list ->
            list.forEach { if (!allAvailable.contains(it)) allAvailable.add(it) }
        }

        // Sort by quality score
        qualityMap.keys.forEach { k ->
            qualityMap[k]!!.sortByDescending { scoreStreamUrl(it) }
        }
        allAvailable.sortByDescending { scoreStreamUrl(it) }

        val result = mutableListOf<VideoSource>()
        // 4 Resolusi lengkap: 360p, 480p, 720p, dan 1080p
        val standardQualities = listOf("360p", "480p", "720p", "1080p")

        for (q in standardQualities) {
            val list = qualityMap[q] ?: emptyList()
            if (list.isNotEmpty()) {
                val mainUrl = list.first()
                val backups = list.drop(1) + allAvailable.filter { it != mainUrl }
                result.add(VideoSource(quality = q, server = q, url = mainUrl, backupUrls = backups))
            } else if (allAvailable.isNotEmpty()) {
                var mainUrl = allAvailable.first()
                if (q == "1080p") {
                    mainUrl = qualityMap["720p"]?.firstOrNull() ?: allAvailable.first()
                } else if (q == "360p") {
                    mainUrl = qualityMap["480p"]?.firstOrNull() ?: allAvailable.first()
                }
                val backups = allAvailable.filter { it != mainUrl }
                result.add(VideoSource(quality = q, server = q, url = mainUrl, backupUrls = backups))
            }
        }
        result
    }
}
