package com.example.launcherapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.launcherapp.domain.repository.AccessControlRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.HashSet

private const val PREFS_NAME = "launcher_access_prefs"
private const val KEY_ALLOWED_PACKAGES = "allowed_packages"
private const val KEY_SELECTION_LOCKED = "selection_locked"

class AccessControlRepositoryImpl(context: Context) : AccessControlRepository {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun getAllowedPackages(): Set<String> = withContext(Dispatchers.IO) {
        val stored = prefs.getStringSet(KEY_ALLOWED_PACKAGES, emptySet()) ?: emptySet()
        HashSet(stored)
    }

    override suspend fun setAllowedPackages(packages: Set<String>) {
        withContext(Dispatchers.IO) {
            prefs.edit().putStringSet(KEY_ALLOWED_PACKAGES, HashSet(packages)).apply()
        }
    }

    override suspend fun isSelectionLocked(): Boolean = withContext(Dispatchers.IO) {
        prefs.getBoolean(KEY_SELECTION_LOCKED, false)
    }

    override suspend fun setSelectionLocked(locked: Boolean) {
        withContext(Dispatchers.IO) {
            prefs.edit().putBoolean(KEY_SELECTION_LOCKED, locked).apply()
        }
    }
}
