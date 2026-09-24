package com.zamnimeku.app.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.zamnimeku.app.data.model.AnimeSource
import com.zamnimeku.app.data.model.HistoryItem
import org.json.JSONArray
import org.json.JSONObject

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("zamnimeku_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PREFERRED_QUALITY = "preferred_quality"
        private const val KEY_WATCHED_ANIMES = "watched_animes_json"
        private const val KEY_SKIPPED_UPDATE_TAG = "skipped_update_tag"
        private const val KEY_NOTIFIED_UPDATE_TAG = "notified_update_tag"
    }

    var preferredQuality: String
        get() = prefs.getString(KEY_PREFERRED_QUALITY, "360p") ?: "360p"
        set(value) = prefs.edit().putString(KEY_PREFERRED_QUALITY, value).apply()

    // Tag release yang user pilih "Nanti" — jangan tawarkan lagi tag yang sama
    var skippedUpdateTag: String
        get() = prefs.getString(KEY_SKIPPED_UPDATE_TAG, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SKIPPED_UPDATE_TAG, value).apply()

    // Tag release yang sudah dikirim notifikasinya — notif sekali per versi
    var notifiedUpdateTag: String
        get() = prefs.getString(KEY_NOTIFIED_UPDATE_TAG, "") ?: ""
        set(value) = prefs.edit().putString(KEY_NOTIFIED_UPDATE_TAG, value).apply()

    // ── PROGRESS PER EPISODE (TIMELINE BAR) ──
    private fun episodeKey(epSlug: String, source: AnimeSource): String = "${source.name}:$epSlug"

    fun saveEpisodeProgress(
        epSlug: String,
        positionMs: Long,
        durationMs: Long,
        source: AnimeSource = AnimeSource.OTAKUDESU
    ) {
        if (durationMs <= 0) return
        val progress = (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        val key = episodeKey(epSlug, source)
        prefs.edit()
            .putFloat("watch_prog_$key", progress)
            .putLong("watch_pos_$key", positionMs)
            .putLong("watch_dur_$key", durationMs)
            .apply()
    }

    fun getEpisodeProgress(epSlug: String, source: AnimeSource = AnimeSource.OTAKUDESU): Float {
        val key = "watch_prog_${episodeKey(epSlug, source)}"
        return if (prefs.contains(key)) prefs.getFloat(key, 0f) else prefs.getFloat("watch_prog_$epSlug", 0f)
    }

    fun getEpisodePosition(epSlug: String, source: AnimeSource = AnimeSource.OTAKUDESU): Long {
        val key = "watch_pos_${episodeKey(epSlug, source)}"
        return if (prefs.contains(key)) prefs.getLong(key, 0L) else prefs.getLong("watch_pos_$epSlug", 0L)
    }

    // ── RIWAYAT ANIME UNIK ──
    fun saveHistory(item: HistoryItem) {
        saveEpisodeProgress(item.lastEpSlug, item.positionMs, item.durationMs, item.source)
        val list = getHistory().toMutableList()
        val existingIndex = list.indexOfFirst {
            it.animeSlug == item.animeSlug && it.source == item.source
        }
        if (existingIndex != -1) {
            list[existingIndex] = item
        } else {
            list.add(0, item)
        }
        // Keep top 100
        val limited = list.take(100)
        val arr = JSONArray()
        for (h in limited) {
            val obj = JSONObject().apply {
                put("animeSlug", h.animeSlug)
                put("animeTitle", h.animeTitle)
                put("animeThumb", h.animeThumb)
                put("lastEpSlug", h.lastEpSlug)
                put("lastEpTitle", h.lastEpTitle)
                put("lastEpIndex", h.lastEpIndex)
                put("positionMs", h.positionMs)
                put("durationMs", h.durationMs)
                put("progress", h.progress.toDouble())
                put("updatedAt", h.updatedAt)
                put("source", h.source.name)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_WATCHED_ANIMES, arr.toString()).apply()
    }

    fun getHistory(): List<HistoryItem> {
        val str = prefs.getString(KEY_WATCHED_ANIMES, null) ?: return emptyList()
        val list = mutableListOf<HistoryItem>()
        try {
            val arr = JSONArray(str)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    HistoryItem(
                        animeSlug = obj.optString("animeSlug"),
                        animeTitle = obj.optString("animeTitle"),
                        animeThumb = obj.optString("animeThumb"),
                        lastEpSlug = obj.optString("lastEpSlug"),
                        lastEpTitle = obj.optString("lastEpTitle"),
                        lastEpIndex = obj.optInt("lastEpIndex", 0),
                        positionMs = obj.optLong("positionMs", 0L),
                        durationMs = obj.optLong("durationMs", 0L),
                        progress = obj.optDouble("progress", 0.0).toFloat(),
                        updatedAt = obj.optLong("updatedAt", 0L),
                        source = runCatching {
                            AnimeSource.valueOf(obj.optString("source"))
                        }.getOrDefault(AnimeSource.MYNIMEKU)
                    )
                )
            }
        } catch (_: Exception) {}
        return list.sortedByDescending { it.updatedAt }
    }

    fun deleteHistoryItem(animeSlug: String, source: AnimeSource = AnimeSource.OTAKUDESU) {
        val list = getHistory().filter { it.animeSlug != animeSlug || it.source != source }
        val arr = JSONArray()
        for (h in list) {
            val obj = JSONObject().apply {
                put("animeSlug", h.animeSlug)
                put("animeTitle", h.animeTitle)
                put("animeThumb", h.animeThumb)
                put("lastEpSlug", h.lastEpSlug)
                put("lastEpTitle", h.lastEpTitle)
                put("lastEpIndex", h.lastEpIndex)
                put("positionMs", h.positionMs)
                put("durationMs", h.durationMs)
                put("progress", h.progress.toDouble())
                put("updatedAt", h.updatedAt)
                put("source", h.source.name)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_WATCHED_ANIMES, arr.toString()).apply()
    }

    fun clearAllHistory() {
        prefs.edit().remove(KEY_WATCHED_ANIMES).apply()
    }
}
