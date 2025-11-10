package com.example.invyucab_project.domain.usecase

import javax.inject.Inject

/**
 * This UseCase does ONE thing: It saves that the user
 * has completed onboarding.
 */
class SaveOnboardingUseCase @Inject constructor(
    private val repository: AppRepository
) {
    suspend operator fun invoke() {
        try {
            repository.saveOnboardingCompleted()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}