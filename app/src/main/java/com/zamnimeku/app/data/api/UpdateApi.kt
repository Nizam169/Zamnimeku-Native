package com.zamnimeku.app.data.api

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val tag: String,
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val notes: String
)

// Cek versi terbaru dari GitHub Releases. Dibandingkan dengan
// BuildConfig.VERSION_CODE (diisi nomor run CI saat release).
object UpdateApi {
    private const val OWNER = "Nizam169"
    private const val REPO = "Zamnimeku-Native"
    private const val LATEST_URL = "https://api.github.com/repos/$OWNER/$REPO/releases/latest"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun checkUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(LATEST_URL)
                .header("User-Agent", "Zamnimeku-App")
                .header("Accept", "application/vnd.github+json")
                .build()
            val body = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                response.body?.string() ?: return@withContext null
            }
            val json = JSONObject(body)
            val tag = json.optString("tag_name")
            if (tag.isEmpty()) return@withContext null
            // Format tag: v1.0.0-<run_number>
            val code = tag.substringAfterLast("-", "").toIntOrNull() ?: return@withContext null

            var apkUrl = ""
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val a = assets.optJSONObject(i) ?: continue
                    val name = a.optString("name")
                    if (name.endsWith(".apk")) {
                        apkUrl = a.optString("browser_download_url")
                        break
                    }
                }
            }
            if (apkUrl.isEmpty()) return@withContext null

            UpdateInfo(
                tag = tag,
                versionCode = code,
                versionName = tag.removePrefix("v"),
                apkUrl = apkUrl,
                notes = json.optString("body")
            )
        } catch (_: Exception) {
            null
        }
    }

    // Download APK dengan progress callback (doneBytes, totalBytes).
    // totalBytes = -1 kalau server tidak mengirim Content-Length.
    // Berhasil HANYA jika ukuran file persis sama dengan Content-Length
    // (mencegah file kepotong yang bikin "paket tidak valid" saat install).
    suspend fun downloadApk(
        url: String,
        dest: File,
        onProgress: (done: Long, total: Long) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Zamnimeku-App")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                val body = response.body ?: return@withContext false
                val total = body.contentLength()
                body.byteStream().use { input ->
                    dest.outputStream().use { output ->
                        val buf = ByteArray(32 * 1024)
                        var done = 0L
                        while (true) {
                            val n = input.read(buf)
                            if (n == -1) break
                            output.write(buf, 0, n)
                            done += n
                            withContext(Dispatchers.Main) { onProgress(done, total) }
                        }
                        output.flush()
                    }
                }
                // Tolak file tidak lengkap
                if (total > 0 && dest.length() != total) {
                    dest.delete()
                    return@withContext false
                }
                if (dest.length() <= 1024 * 1024) {
                    dest.delete()
                    return@withContext false
                }
                return@withContext true
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }
}
