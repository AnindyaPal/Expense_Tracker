package com.example.expensetracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MutualFundScreenState(
    val selectedFunds: List<MutualFund> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<MutualFund> = emptyList(),
    val isLoading: Boolean = false,
    val overlapResult: OverlapData? = null,
    val error: String? = null,
    val searchHint: String? = "Start by typing an AMC name (e.g., HDFC, ICICI, SBI)",
    val currentTab: MutualFundTab = MutualFundTab.SELECTION
)

enum class MutualFundTab {
    SELECTION, COMPARISON
}

@HiltViewModel
class MutualFundViewModel @Inject constructor(
    private val storage: MutualFundStorage
) : ViewModel() {
    private val _state = MutableStateFlow(MutualFundScreenState())
    val state: StateFlow<MutualFundScreenState> = _state

    private val searchQueryFlow = MutableStateFlow("")

    init {
        setupSearchQueryDebounce()
        loadSavedFunds()
    }

    private fun loadSavedFunds() {
        viewModelScope.launch {
            val savedFunds = storage.getSelectedFunds()
            _state.value = _state.value.copy(selectedFunds = savedFunds)
        }
    }

    fun setCurrentTab(tab: MutualFundTab) {
        viewModelScope.launch {
            if (tab == MutualFundTab.COMPARISON && _state.value.selectedFunds.size >= 2) {
                compareSelectedFunds()
            }
            _state.value = _state.value.copy(currentTab = tab)
        }
    }

    @OptIn(FlowPreview::class)
    private fun setupSearchQueryDebounce() {
        searchQueryFlow
            .debounce(300)
            .onEach { query ->
                if (query.length >= 3) {
                    searchMutualFunds(query)
                } else {
                    _state.value = _state.value.copy(
                        searchResults = emptyList(),
                        searchHint = "Type at least 3 characters"
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChange(query: String) {
        _state.value = _state.value.copy(
            searchQuery = query,
            searchHint = if (query.isEmpty()) "Start by typing an AMC name" else null
        )
        searchQueryFlow.value = query
    }

    private fun searchMutualFunds(query: String) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(
                    isLoading = true,
                    searchHint = null
                )
                val response = RetrofitClient.mutualFundService.searchMutualFunds(
                    mapOf("search_term" to query)
                )

                val validResults = response.data.filter {
                    it.fund_name != "No Result Found"
                }

                _state.value = _state.value.copy(
                    searchResults = validResults,
                    isLoading = false,
                    error = null,
                    searchHint = if (validResults.isEmpty()) "No funds found for '$query'" else null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to search mutual funds: ${e.message}",
                    searchHint = "An error occurred. Please try again.",
                    searchResults = emptyList()
                )
            }
        }
    }

    fun addMutualFund(fund: MutualFund) {
        val currentFunds = _state.value.selectedFunds
        if (currentFunds.size < 5 && !currentFunds.contains(fund)) {
            val updatedFunds = currentFunds + fund
            _state.value = _state.value.copy(
                selectedFunds = updatedFunds,
                searchQuery = "",
                searchResults = emptyList(),
                searchHint = "Start by typing an AMC name (e.g., HDFC, ICICI, SBI)"
            )
            // Save to storage
            viewModelScope.launch {
                storage.saveSelectedFunds(updatedFunds)
            }
        }
    }

    fun removeMutualFund(fund: MutualFund) {
        val updatedFunds = _state.value.selectedFunds - fund
        _state.value = _state.value.copy(
            selectedFunds = updatedFunds,
            overlapResult = null
        )
        // Save to storage
        viewModelScope.launch {
            storage.saveSelectedFunds(updatedFunds)
        }
    }

    fun compareSelectedFunds() {
        viewModelScope.launch {
            try {
                if (_state.value.selectedFunds.size < 2) {
                    _state.value = _state.value.copy(
                        error = "Please select at least 2 funds to compare"
                    )
                    return@launch
                }

                _state.value = _state.value.copy(
                    isLoading = true,
                    error = null,
                    currentTab = MutualFundTab.COMPARISON
                )

                // Handle multiple funds comparison
                val request = OverlapRequest(
                    isin = _state.value.selectedFunds.map { it.sd_scheme_isin }
                )

                val response = RetrofitClient.mutualFundService.calculateOverlap(request)

                if (response.meta.status == 200) {
                    // Check if data can handle multiple funds
                    val overlapResults = response.data

                    _state.value = _state.value.copy(
                        overlapResult = overlapResults,
                        isLoading = false,
                        error = null
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Failed to calculate overlap: ${response.meta.message}",
                        currentTab = MutualFundTab.SELECTION
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to calculate overlap: ${e.localizedMessage}",
                    currentTab = MutualFundTab.SELECTION
                )
            }
        }
    }

    fun clearSelectedFunds() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                selectedFunds = emptyList(),
                overlapResult = null
            )
            storage.saveSelectedFunds(emptyList())
        }
    }
}