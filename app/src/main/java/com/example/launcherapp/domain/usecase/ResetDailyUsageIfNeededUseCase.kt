package com.example.launcherapp.domain.usecase

import com.example.launcherapp.domain.repository.AppUsageRepository

class ResetDailyUsageIfNeededUseCase(private val repository: AppUsageRepository) {
    suspend operator fun invoke(currentTimeMillis: Long) {
        repository.resetDailyUsageIfNeeded(currentTimeMillis)
    }
}
