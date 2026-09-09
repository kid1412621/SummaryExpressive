package me.nanova.summaryexpressive.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.nanova.summaryexpressive.domain.usecase.GetOnboardingStatusUseCase
import me.nanova.summaryexpressive.domain.usecase.SetOnboardingStatusUseCase
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    getOnboardingStatusUseCase: GetOnboardingStatusUseCase,
    private val setOnboardingStatusUseCase: SetOnboardingStatusUseCase,
) : ViewModel() {

    val isOnboarded: StateFlow<Boolean?> = getOnboardingStatusUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _appStartAction = MutableStateFlow(AppStartAction())
    val appStartAction: StateFlow<AppStartAction> = _appStartAction.asStateFlow()

    fun onEvent(action: AppStartAction) {
        _appStartAction.value = action
    }

    fun onStartActionHandled() {
        _appStartAction.value = AppStartAction()
    }

    fun setIsOnboarded(newValue: Boolean) {
        viewModelScope.launch {
            setOnboardingStatusUseCase(newValue)
        }
    }
}
