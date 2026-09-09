package me.nanova.summaryexpressive.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.nanova.summaryexpressive.domain.usecase.SummarizeContentUseCase
import me.nanova.summaryexpressive.exception.SummaryException
import me.nanova.summaryexpressive.model.SummaryLength
import javax.inject.Inject

@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val summarizeContentUseCase: SummarizeContentUseCase,
) : ViewModel() {

    private val _summarizationState = MutableStateFlow(SummarizationState())
    val summarizationState: StateFlow<SummarizationState> = _summarizationState.asStateFlow()

    private var currentInput: String? = null

    fun clearCurrentSummary() {
        currentInput = null
        _summarizationState.update {
            it.copy(
                summaryResult = null,
                error = null,
                lengthResults = emptyMap()
            )
        }
    }

    fun switchLength(length: SummaryLength) {
        if (_summarizationState.value.isLoading) return
        val currentResults = _summarizationState.value.lengthResults.ifEmpty {
            val current = _summarizationState.value.summaryResult
            if (current?.length != null) mapOf(current.length to current) else emptyMap()
        }
        if (currentResults.isEmpty()) return
        val cachedResult = currentResults[length]
        _summarizationState.update {
            it.copy(
                summaryResult = cachedResult,
                lengthResults = if (it.lengthResults.isEmpty() && currentResults.isNotEmpty()) currentResults else it.lengthResults,
                error = null
            )
        }
    }

    fun summarize(text: String, overrideLength: SummaryLength? = null) {
        viewModelScope.launch {
            if (currentInput != text) {
                currentInput = text
                _summarizationState.update {
                    it.copy(
                        isLoading = true,
                        summaryResult = null,
                        error = null,
                        lengthResults = emptyMap()
                    )
                }
            } else {
                _summarizationState.update {
                    it.copy(
                        isLoading = true,
                        error = null
                    )
                }
            }

            summarizeContentUseCase(text, overrideLength)
                .onSuccess { output ->
                    _summarizationState.update {
                        val updatedLengthResults = it.lengthResults + (output.length to output)
                        it.copy(
                            summaryResult = output,
                            lengthResults = updatedLengthResults,
                            isLoading = false,
                            error = null,
                        )
                    }
                }
                .onFailure { throwable ->
                    val error = throwable as? SummaryException
                        ?: SummaryException.UnknownException(
                            throwable.message ?: "An unknown error occurred."
                        )
                    _summarizationState.update {
                        it.copy(
                            isLoading = false,
                            error = error,
                        )
                    }
                }
        }
    }
}