package com.example.launcherapp.domain.usecase

import com.example.launcherapp.domain.repository.AccessControlRepository

class UpdateAllowedPackagesUseCase(private val repository: AccessControlRepository) {
    suspend operator fun invoke(packages: Set<String>) {
        repository.setAllowedPackages(packages)
    }
}
