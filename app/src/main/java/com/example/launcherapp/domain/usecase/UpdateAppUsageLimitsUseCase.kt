package com.example.launcherapp.domain.usecase

import com.example.launcherapp.domain.repository.AppUsageRepository

class UpdateAppUsageLimitsUseCase(private val repository: AppUsageRepository) {
    suspend operator fun invoke(limitsMinutes: Map<String, Int?>) {
        repository.updateUsageLimits(limitsMinutes)
    }
}
