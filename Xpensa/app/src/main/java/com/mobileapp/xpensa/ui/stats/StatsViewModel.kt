package com.mobileapp.xpensa.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileapp.xpensa.data.api.StatsApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StatsViewModel(
    private val statsApi: StatsApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadDayStats()
    }

    fun selectPeriod(period: StatisticsPeriod) {
        if (_uiState.value.selectedPeriod == period) {
            return
        }

        _uiState.value = _uiState.value.copy(
            selectedPeriod = period,
            error = null
        )

        when (period) {
            StatisticsPeriod.DAY -> loadDayStats()
            StatisticsPeriod.MONTH -> loadMonthStats()
        }
    }

    fun loadDayStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                val response = statsApi.getDayStats()

                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = _uiState.value.copy(
                        dayStats = response.body(),
                        isLoading = false,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Unable to load today's statistics (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unable to load today's statistics"
                )
            }
        }
    }

    fun loadMonthStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                val response = statsApi.getMonthStats()

                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = _uiState.value.copy(
                        monthStats = response.body(),
                        isLoading = false,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Unable to load monthly statistics (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unable to load monthly statistics"
                )
            }
        }
    }

    fun retry() {
        when (_uiState.value.selectedPeriod) {
            StatisticsPeriod.DAY -> loadDayStats()
            StatisticsPeriod.MONTH -> loadMonthStats()
        }
    }
}