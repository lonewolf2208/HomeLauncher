package com.example.launcherapp.domain.usecase

import com.example.launcherapp.domain.repository.AccessControlRepository

class IsSelectionLockedUseCase(private val repository: AccessControlRepository) {
    suspend operator fun invoke(): Boolean = repository.isSelectionLocked()
}
