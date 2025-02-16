package com.example.expensetracker

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class NetWorthViewModel @Inject constructor() : ViewModel() {
    private val _selectedTab = MutableStateFlow(NetWorthTab.INVESTMENTS)
    val selectedTab: StateFlow<NetWorthTab> = _selectedTab

    fun onTabSelected(tab: NetWorthTab) {
        _selectedTab.value = tab
    }
}

enum class NetWorthTab {
    INVESTMENTS, LOANS, ASSETS
}