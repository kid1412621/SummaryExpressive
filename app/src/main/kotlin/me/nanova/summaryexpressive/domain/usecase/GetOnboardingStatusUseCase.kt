package me.nanova.summaryexpressive.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.nanova.summaryexpressive.domain.repository.UserPreferencesRepository
import javax.inject.Inject

class GetOnboardingStatusUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    operator fun invoke(): Flow<Boolean?> {
        return userPreferencesRepository.preferencesFlow.map { it.isOnboarded }
    }
}
