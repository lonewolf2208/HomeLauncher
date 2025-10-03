package com.example.launcherapp.data.usage

import android.content.Context
import android.content.SharedPreferences
import com.example.launcherapp.domain.model.AppUsageSnapshot
import com.example.launcherapp.domain.repository.AppUsageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Calendar

private const val PREFS_NAME = "launcher_usage_prefs"
private const val KEY_USAGE_DATA = "usage_data"
private const val KEY_LAST_CHECK = "last_usage_check"

class AppUsageRepositoryImpl(context: Context) : AppUsageRepository {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun getUsageSnapshots(): Map<String, AppUsageSnapshot> = withContext(Dispatchers.IO) {
        val json = prefs.getString(KEY_USAGE_DATA, null) ?: return@withContext emptyMap()
        val root = JSONObject(json)
        val packages = root.optJSONObject("packages") ?: return@withContext emptyMap()
        val result = mutableMapOf<String, AppUsageSnapshot>()
        packages.keys().forEach { pkg ->
            val obj = packages.getJSONObject(pkg)
            val limitMinutes = if (obj.has("limit")) obj.optInt("limit", -1).takeIf { it >= 0 } else null
            val usedMillis = obj.optLong("used", 0L)
            result[pkg] = AppUsageSnapshot(pkg, limitMinutes, usedMillis)
        }
        result
    }

    override suspend fun updateUsageLimits(limitsMinutes: Map<String, Int?>) {
        withContext(Dispatchers.IO) {
            val current = JSONObject(prefs.getString(KEY_USAGE_DATA, null) ?: defaultPayload())
            val packages = current.optJSONObject("packages") ?: JSONObject()
            limitsMinutes.forEach { (pkg, limit) ->
                val obj = packages.optJSONObject(pkg) ?: JSONObject()
                if (limit != null) {
                    obj.put("limit", limit)
                } else {
                    obj.remove("limit")
                }
                packages.put(pkg, obj)
            }
            current.put("packages", packages)
            prefs.edit().putString(KEY_USAGE_DATA, current.toString()).apply()
        }
    }

    override suspend fun recordUsageDelta(deltaMillisByPackage: Map<String, Long>) {
        if (deltaMillisByPackage.isEmpty()) return
        withContext(Dispatchers.IO) {
            val current = JSONObject(prefs.getString(KEY_USAGE_DATA, null) ?: defaultPayload())
            val packages = current.optJSONObject("packages") ?: JSONObject()
            deltaMillisByPackage.forEach { (pkg, delta) ->
                val obj = packages.optJSONObject(pkg) ?: JSONObject()
                val newUsed = obj.optLong("used", 0L) + delta
                obj.put("used", newUsed.coerceAtLeast(0L))
                packages.put(pkg, obj)
            }
            current.put("packages", packages)
            prefs.edit().putString(KEY_USAGE_DATA, current.toString()).apply()
        }
    }

    override suspend fun resetDailyUsageIfNeeded(currentTimeMillis: Long) {
        withContext(Dispatchers.IO) {
            val nowDay = dayKey(currentTimeMillis)
            val current = JSONObject(prefs.getString(KEY_USAGE_DATA, null) ?: defaultPayload())
            val storedDay = current.optString("day", nowDay)
            if (storedDay != nowDay) {
                val packages = current.optJSONObject("packages") ?: JSONObject()
                packages.keys().forEach { pkg ->
                    val obj = packages.getJSONObject(pkg)
                    obj.remove("used")
                    packages.put(pkg, obj)
                }
                current.put("packages", packages)
                current.put("day", nowDay)
                prefs.edit().putString(KEY_USAGE_DATA, current.toString()).apply()
            }
        }
    }

    override suspend fun setLastUsageCheck(timestampMillis: Long) {
        withContext(Dispatchers.IO) {
            prefs.edit().putLong(KEY_LAST_CHECK, timestampMillis).apply()
        }
    }

    override suspend fun getLastUsageCheck(): Long = withContext(Dispatchers.IO) {
        prefs.getLong(KEY_LAST_CHECK, 0L)
    }

    private fun defaultPayload(): String {
        val day = dayKey(System.currentTimeMillis())
        return JSONObject()
            .put("day", day)
            .put("packages", JSONObject())
            .toString()
    }

    private fun dayKey(timeMillis: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timeMillis }
        val year = calendar.get(Calendar.YEAR)
        val day = calendar.get(Calendar.DAY_OF_YEAR)
        return "$year-$day"
    }
}
