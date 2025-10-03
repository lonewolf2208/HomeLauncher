package com.example.launcherapp.domain.usecase

import com.example.launcherapp.domain.repository.AccessControlRepository

class GetAllowedPackagesUseCase(private val repository: AccessControlRepository) {
    suspend operator fun invoke(): Set<String> = repository.getAllowedPackages()
}
