package com.example.launcherapp.domain.usecase

import com.example.launcherapp.domain.repository.AppUsageRepository

class SetLastUsageCheckUseCase(private val repository: AppUsageRepository) {
    suspend operator fun invoke(timestampMillis: Long) {
        repository.setLastUsageCheck(timestampMillis)
    }
}
