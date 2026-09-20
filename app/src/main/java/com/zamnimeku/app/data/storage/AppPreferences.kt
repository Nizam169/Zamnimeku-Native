package com.zamnimeku.app.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.zamnimeku.app.data.model.HistoryItem
import org.json.JSONArray
import org.json.JSONObject

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("zamnimeku_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PREFERRED_QUALITY = "preferred_quality"
        private const val KEY_WATCHED_ANIMES = "watched_animes_json"
        private const val KEY_BOOKMARKS = "manga_bookmarks_json"
    }

    var preferredQuality: String
        get() = prefs.getString(KEY_PREFERRED_QUALITY, "360p") ?: "360p"
        set(value) = prefs.edit().putString(KEY_PREFERRED_QUALITY, value).apply()

    fun saveHistory(item: HistoryItem) {
        val list = getHistory().toMutableList()
        val existingIndex = list.indexOfFirst { it.animeSlug == item.animeSlug }
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
                        updatedAt = obj.optLong("updatedAt", 0L)
                    )
                )
            }
        } catch (_: Exception) {}
        return list.sortedByDescending { it.updatedAt }
    }

    fun deleteHistoryItem(animeSlug: String) {
        val list = getHistory().filter { it.animeSlug != animeSlug }
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
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_WATCHED_ANIMES, arr.toString()).apply()
    }

    fun clearAllHistory() {
        prefs.edit().remove(KEY_WATCHED_ANIMES).apply()
    }
}
