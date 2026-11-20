package dev.arnoldsemase.calculator.data

import android.content.SharedPreferences
import dev.arnoldsemase.calculator.engine.RateTable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Offline-first exchange rates. The last good table is kept in a file and shown straight away;
 * [refresh] downloads a new one when the user is online. Nothing else in the app touches the network.
 */
class RatesRepository(
    private val cacheFile: File,
    private val prefs: SharedPreferences,
) {
    suspend fun cached(): RateTable? = withContext(Dispatchers.IO) {
        try {
            if (!cacheFile.exists()) return@withContext null
            val json = JSONObject(cacheFile.readText())
            RateTable(json.getLong("fetchedAt"), readRates(json.getJSONObject("rates")))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    suspend fun refresh(): Result<RateTable> = withContext(Dispatchers.IO) {
        val connection = (URL(RATES_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            requestMethod = "GET"
        }
        try {
            check(connection.responseCode == 200) { "HTTP ${connection.responseCode}" }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            check(json.optString("result") == "success") { "Rates feed reported a problem" }
            val rates = readRates(json.getJSONObject("rates"))
            check(rates["USD"] == 1.0 && rates.size > 10) { "Rates feed looks incomplete" }
            val table = RateTable(System.currentTimeMillis(), rates)
            write(table)
            Result.success(table)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection.disconnect()
        }
    }

    /** The currencies the user chose, in order, and which one they were typing into. */
    fun selection(): Pair<List<String>, String> {
        val list = prefs.getString(KEY_SELECTED, null)
            ?.split(',')?.filter { it.length == 3 }?.takeIf { it.size >= 2 }
            ?: DEFAULT_SELECTED
        val active = prefs.getString(KEY_ACTIVE, null)?.takeIf { it in list } ?: list.first()
        return list to active
    }

    fun saveSelection(selected: List<String>, active: String) {
        prefs.edit().putString(KEY_SELECTED, selected.joinToString(",")).putString(KEY_ACTIVE, active).apply()
    }

    private fun readRates(obj: JSONObject): Map<String, Double> {
        val out = HashMap<String, Double>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val v = obj.optDouble(k, Double.NaN)
            if (v.isFinite() && v > 0.0) out[k] = v
        }
        return out
    }

    private fun write(table: RateTable) {
        val json = JSONObject()
            .put("fetchedAt", table.fetchedAtMillis)
            .put("rates", JSONObject(table.rates as Map<*, *>))
        val tmp = File(cacheFile.parentFile, cacheFile.name + ".tmp")
        tmp.writeText(json.toString())
        if (!tmp.renameTo(cacheFile)) {
            cacheFile.writeText(json.toString())
            tmp.delete()
        }
    }

    companion object {
        private const val RATES_URL = "https://open.er-api.com/v6/latest/USD"
        private const val KEY_SELECTED = "selected"
        private const val KEY_ACTIVE = "active"
        val DEFAULT_SELECTED = listOf("USD", "EUR", "ZAR", "UGX")
    }
}
