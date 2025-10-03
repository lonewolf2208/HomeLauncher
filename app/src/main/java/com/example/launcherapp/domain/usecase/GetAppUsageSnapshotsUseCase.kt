package com.example.launcherapp.domain.usecase

import com.example.launcherapp.domain.model.AppUsageSnapshot
import com.example.launcherapp.domain.repository.AppUsageRepository

class GetAppUsageSnapshotsUseCase(private val repository: AppUsageRepository) {
    suspend operator fun invoke(): Map<String, AppUsageSnapshot> = repository.getUsageSnapshots()
}
