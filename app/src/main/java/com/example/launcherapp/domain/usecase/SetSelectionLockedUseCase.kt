package com.example.launcherapp.domain.usecase

import com.example.launcherapp.domain.repository.AccessControlRepository

class SetSelectionLockedUseCase(private val repository: AccessControlRepository) {
    suspend operator fun invoke(locked: Boolean) {
        repository.setSelectionLocked(locked)
    }
}
