package com.example.aiban_rammanager.ui

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

class StatsRepository(context: Context) {

    private val prefs = context.getSharedPreferences(
        "aiban_ram_history",
        Context.MODE_PRIVATE
    )

    private val lock = Any()

    // Список держим в памяти, чтобы не разбирать JSON при каждой записи
    private var cache: List<RamSample>? = null

    fun snapshot(): List<RamSample> = synchronized(lock) { loadLocked() }

    private fun loadLocked(): List<RamSample> {
        cache?.let { return it }

        val raw = prefs.getString("samples", "[]") ?: "[]"
        val array = try {
            JSONArray(raw)
        } catch (e: Exception) {
            JSONArray()
        }

        val result = mutableListOf<RamSample>()

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue

            result.add(
                RamSample(
                    timeMs = item.optLong("timeMs"),
                    usedKb = item.optLong("usedKb"),
                    totalKb = item.optLong("totalKb")
                )
            )
        }

        cache = result
        return result
    }

    fun add(sample: RamSample) {
        synchronized(lock) {
            val samples = (loadLocked() + sample).takeLast(MAX_SAMPLES)
            val array = JSONArray()

            samples.forEach {
                array.put(
                    JSONObject()
                        .put("timeMs", it.timeMs)
                        .put("usedKb", it.usedKb)
                        .put("totalKb", it.totalKb)
                )
            }

            prefs.edit { putString("samples", array.toString()) }
            cache = samples
        }
    }

    fun clear() {
        synchronized(lock) {
            prefs.edit { remove("samples") }
            cache = emptyList()
        }
    }

    private companion object {
        // 1440 точек по одной в 30 секунд — это 12 часов
        const val MAX_SAMPLES = 1440
    }
}
