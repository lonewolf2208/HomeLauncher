package com.example.launcherapp.domain.usecase

import com.example.launcherapp.domain.repository.AppUsageRepository

class RecordAppUsageDeltaUseCase(private val repository: AppUsageRepository) {
    suspend operator fun invoke(deltaMillisByPackage: Map<String, Long>) {
        repository.recordUsageDelta(deltaMillisByPackage)
    }
}
