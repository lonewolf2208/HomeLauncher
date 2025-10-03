package com.example.launcherapp.domain.repository

interface AccessControlRepository {
    suspend fun getAllowedPackages(): Set<String>
    suspend fun setAllowedPackages(packages: Set<String>)
    suspend fun isSelectionLocked(): Boolean
    suspend fun setSelectionLocked(locked: Boolean)
}
