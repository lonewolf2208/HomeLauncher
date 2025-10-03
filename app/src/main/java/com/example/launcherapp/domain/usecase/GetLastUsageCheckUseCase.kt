package com.example.launcherapp.domain.usecase

import com.example.launcherapp.domain.repository.AppUsageRepository

class GetLastUsageCheckUseCase(private val repository: AppUsageRepository) {
    suspend operator fun invoke(): Long = repository.getLastUsageCheck()
}
