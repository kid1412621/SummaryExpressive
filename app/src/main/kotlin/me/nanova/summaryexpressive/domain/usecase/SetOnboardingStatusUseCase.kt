package me.nanova.summaryexpressive.domain.usecase

import me.nanova.summaryexpressive.domain.repository.UserPreferencesRepository
import javax.inject.Inject

class SetOnboardingStatusUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    suspend operator fun invoke(isOnboarded: Boolean) {
        userPreferencesRepository.setIsOnboarded(isOnboarded)
    }
}
