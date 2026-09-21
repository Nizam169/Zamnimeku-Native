package com.zamnimeku.app.data.api

import com.zamnimeku.app.data.model.MangaCard
import com.zamnimeku.app.data.model.MangaChapter
import com.zamnimeku.app.data.model.MangaDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object MangaApi {
    private const val BASE_URL = "https://www.mynimeku.com"
    private const val THUMB_FALLBACK = "https://www.mynimeku.com/wp-content/uploads/2024/07/icon-mynimeku.avif"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", USER_AGENT)
                .header("Referer", "https://www.mynimeku.com/")
                .build()
            chain.proceed(req)
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

    private fun getThumb(item: JSONObject): String {
        val embedded = item.optJSONObject("_embedded")
        if (embedded != null) {
            val media = embedded.optJSONArray("wp:featuredmedia")
            if (media != null && media.length() > 0) {
                val src = media.optJSONObject(0)?.optString("source_url")
                if (!src.isNullOrEmpty()) return src
            }
        }
        val rendered = item.optJSONObject("content")?.optString("rendered") ?: ""
        val m = Pattern.compile("""<img[^>]+src=["']([^"']+)["']""").matcher(rendered)
        if (m.find()) {
            return m.group(1)!!
        }
        return THUMB_FALLBACK
    }

    suspend fun listKomik(page: Int = 1, perPage: Int = 24): List<MangaCard> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/wp-json/wp/v2/komik?per_page=$perPage&page=$page&_embed=1"
        val jsonStr = getHtml(url)
        if (jsonStr.isEmpty()) return@withContext emptyList()

        val list = mutableListOf<MangaCard>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val slug = obj.optString("slug")
                val titleObj = obj.optJSONObject("title")
                val title = titleObj?.optString("rendered") ?: slug
                val link = obj.optString("link")
                val thumb = getThumb(obj)

                list.add(
                    MangaCard(
                        title = Jsoup.parse(title).text(),
                        slug = slug,
                        url = link,
                        thumb = thumb
                    )
                )
            }
        } catch (_: Exception) {}
        list
    }

    suspend fun searchKomik(keyword: String, perPage: Int = 24): List<MangaCard> = withContext(Dispatchers.IO) {
        val encoded = java.net.URLEncoder.encode(keyword, "UTF-8")
        val url = "$BASE_URL/wp-json/wp/v2/komik?search=$encoded&per_page=$perPage&_embed=1"
        val jsonStr = getHtml(url)
        if (jsonStr.isEmpty()) return@withContext emptyList()

        val list = mutableListOf<MangaCard>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val slug = obj.optString("slug")
                val titleObj = obj.optJSONObject("title")
                val title = titleObj?.optString("rendered") ?: slug
                val link = obj.optString("link")
                val thumb = getThumb(obj)

                list.add(
                    MangaCard(
                        title = Jsoup.parse(title).text(),
                        slug = slug,
                        url = link,
                        thumb = thumb
                    )
                )
            }
        } catch (_: Exception) {}
        list
    }

    suspend fun getMangaDetail(komikSlug: String): MangaDetail? = withContext(Dispatchers.IO) {
        val html = getHtml("$BASE_URL/komik/$komikSlug/")
        if (html.isEmpty()) return@withContext null

        val doc = Jsoup.parse(html)
        val title = doc.selectFirst("h1.komik-series-title")?.text()?.trim() ?: komikSlug
        val thumb = doc.selectFirst("div.komik-series-cover img")?.attr("src") ?: THUMB_FALLBACK
        val synopsis = doc.select("div.komik-series-synopsis p").text().trim()
        val status = doc.selectFirst("div.komik-series-meta:contains(Status)")?.text()?.replace("Status:", "")?.trim() ?: "Ongoing"
        val type = doc.selectFirst("div.komik-series-meta:contains(Type)")?.text()?.replace("Type:", "")?.trim() ?: "Manga"
        val author = doc.selectFirst("div.komik-series-meta:contains(Author)")?.text()?.replace("Author:", "")?.trim() ?: "-"

        val genres = doc.select("div.komik-series-genres a").map { it.text().trim() }
        val chapters = getChapters(komikSlug)

        MangaDetail(
            title = title,
            slug = komikSlug,
            thumb = thumb,
            synopsis = synopsis,
            status = status,
            type = type,
            author = author,
            genres = genres,
            chapters = chapters
        )
    }

    suspend fun getChapters(komikSlug: String): List<MangaChapter> = withContext(Dispatchers.IO) {
        val html = getHtml("$BASE_URL/komik/$komikSlug/")
        if (html.isEmpty()) return@withContext emptyList()

        val doc = Jsoup.parse(html)
        val rows = doc.select("div.komik-series-chapter-row")
        val chapters = mutableListOf<MangaChapter>()

        for (row in rows) {
            val a = row.selectFirst("a.komik-series-chapter-item") ?: continue
            val href = a.attr("href")
            val title = a.selectFirst("span.komik-series-chapter-item__title")?.text()?.trim() ?: ""
            val date = a.selectFirst("span.komik-series-chapter-item__date")?.text()?.trim() ?: ""
            val slug = href.trimEnd('/').substringAfterLast('/')

            if (title.isNotEmpty()) {
                chapters.add(
                    MangaChapter(
                        title = Jsoup.parse(title).text(),
                        slug = slug,
                        url = href,
                        date = date
                    )
                )
            }
        }
        chapters
    }

    // Ambil URL gambar TERBESAR dari satu tag <img>:
    // WordPress lazy-load sering menaruh placeholder kecil di "src",
    // sedangkan gambar asli ada di "srcset"/"data-src"/"data-lazy-src".
    private fun bestImgUrl(img: Element): String? {
        fun largestFromSrcset(srcset: String): String? {
            var bestUrl: String? = null
            var bestScore = -1
            for (part in srcset.split(",")) {
                val tokens = part.trim().split(Regex("\\s+"))
                if (tokens.isEmpty()) continue
                val url = tokens[0]
                if (!url.startsWith("http")) continue
                var score = 0
                if (tokens.size > 1) {
                    val d = tokens[1]
                    score = when {
                        d.endsWith("w") -> d.dropLast(1).toIntOrNull() ?: 0
                        d.endsWith("x") -> ((d.dropLast(1).toFloatOrNull() ?: 0f) * 1000).toInt()
                        else -> 0
                    }
                }
                if (score >= bestScore) {
                    bestScore = score
                    bestUrl = url
                }
            }
            return bestUrl
        }

        // 1. srcset terbesar dulu (gambar full)
        for (attr in listOf("srcset", "data-srcset", "data-lazy-srcset")) {
            val v = img.attr(attr)
            if (v.isNotEmpty()) {
                val best = largestFromSrcset(v)
                if (best != null && isContentImage(best)) return best
            }
        }
        // 2. Atribut lazy-load / original
        for (attr in listOf("data-src", "data-lazy-src", "data-original", "data-full-url", "src")) {
            val v = img.attr(attr).trim()
            if (v.startsWith("http") && isContentImage(v)) return v
        }
        return null
    }

    private fun isContentImage(url: String): Boolean {
        if (!url.startsWith("http")) return false
        if (url.startsWith("data:")) return false
        val l = url.lowercase()
        if (l.contains("icon-mynimeku") || l.contains("logo")) return false
        if (l.contains("avatar") || l.contains("emoticon") || l.contains("smiley")) return false
        if (l.contains("blank.gif") || l.contains("lazyload") || l.contains("placeholder")) return false
        return true
    }

    suspend fun getChapterImages(chapterSlug: String): List<String> = withContext(Dispatchers.IO) {
        val images = mutableListOf<String>()

        fun addIfNew(url: String?) {
            if (url != null && !images.contains(url)) images.add(url)
        }

        // 1. Ambil dari WP REST API
        try {
            val url = "$BASE_URL/wp-json/wp/v2/chapter?slug=$chapterSlug&_embed=1"
            val jsonStr = getHtml(url)
            if (jsonStr.isNotEmpty()) {
                val arr = JSONArray(jsonStr)
                if (arr.length() > 0) {
                    val obj = arr.getJSONObject(0)
                    val rendered = obj.optJSONObject("content")?.optString("rendered") ?: ""
                    val doc = Jsoup.parse(rendered)
                    for (img in doc.select("img")) {
                        addIfNew(bestImgUrl(img))
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Fallback: Parse halaman HTML chapter langsung.
        //    Prioritas area bacaan dulu supaya tidak kecampur logo/banner
        //    (yang kecil-kecil dan bikin halaman kelihatan sempit).
        if (images.isEmpty()) {
            try {
                val html = getHtml("$BASE_URL/chapter/$chapterSlug/")
                if (html.isNotEmpty()) {
                    val doc = Jsoup.parse(html)
                    val scoped = doc.select("div.main-reading-area img, div.reader-area img, div.entry-content img")
                    val targets = if (scoped.isNotEmpty()) scoped else doc.select("img")
                    for (img in targets) {
                        addIfNew(bestImgUrl(img))
                    }
                }
            } catch (_: Exception) {}
        }
        images
    }
}
