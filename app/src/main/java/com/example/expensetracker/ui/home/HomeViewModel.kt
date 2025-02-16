package com.example.expensetracker.ui.home

import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.Expense
import com.example.expensetracker.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.YearMonth
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    private val _selectedPeriod = MutableStateFlow(Period.TODAY)
    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _sortOrder = MutableStateFlow(SortOrder.RECENT_FIRST)

    val selectedPeriod = _selectedPeriod.asStateFlow()
    val selectedMonth = _selectedMonth.asStateFlow()
    val selectedCategory = _selectedCategory.asStateFlow()
    val sortOrder = _sortOrder.asStateFlow()

    // First filter by period
    val filteredExpenses = combine(
        _expenses,
        _selectedPeriod,
        _selectedMonth
    ) { expenses, period, month ->
        filterExpensesByPeriod(expenses, period, month)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Then apply category filter and sorting
    val displayedExpenses = combine(
        filteredExpenses,
        _selectedCategory,
        _sortOrder
    ) { expenses, category, sortOrder ->
        var filtered = when (category) {
            null -> expenses
            else -> expenses.filter { it.category == category }
        }

        filtered = when (sortOrder) {
            SortOrder.RECENT_FIRST -> filtered.sortedByDescending { it.date }
            SortOrder.AMOUNT_HIGH_TO_LOW -> filtered.sortedByDescending { it.amount }
            SortOrder.AMOUNT_LOW_TO_HIGH -> filtered.sortedBy { it.amount }
        }

        filtered
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val expensesByCategory = filteredExpenses
        .map { expensesList ->
            expensesList.groupBy {
                // Normalize category names
                when (it.category.lowercase()) {
                    "misc", "miscellaneous", "others", "other" -> "Misc"
                    "food", "restaurants", "dining" -> "Food"
                    "grocery", "groceries", "supermarket" -> "Grocery"
                    "recharge", "mobile", "phone" -> "Recharge"
                    else -> it.category
                }
            }.mapValues { (_, expenses) ->
                expenses.sumOf { it.amount }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    val periodTotal = filteredExpenses
        .map { expenses ->
            expenses.sumOf { it.amount }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val selectedCategoryTotal = combine(
        filteredExpenses,
        _selectedCategory
    ) { expenses, category ->
        when (category) {
            null -> null
            else -> expenses.filter { it.category == category }.sumOf { it.amount }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val availableMonths = _expenses
        .map { expenses ->
            expenses.map { YearMonth.from(it.date) }.distinct().sortedDescending()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            val startOfYear = LocalDateTime.now()
                .withDayOfYear(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)

            expenseRepository.getExpensesByDateRange(startOfYear, LocalDateTime.now())
                .collect { expensesList ->
                    _expenses.value = expensesList
                }
        }
    }

    fun setPeriod(period: Period) {
        _selectedPeriod.value = period
    }

    fun setMonth(yearMonth: YearMonth) {
        _selectedMonth.value = yearMonth
        if (_selectedPeriod.value != Period.CUSTOM_MONTH) {
            _selectedPeriod.value = Period.CUSTOM_MONTH
        }
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
        // Reset sort order when changing categories
        _sortOrder.value = SortOrder.RECENT_FIRST
    }

    fun setSort(order: SortOrder) {
        _sortOrder.value = order
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun filterExpensesByPeriod(
        expenses: List<Expense>,
        period: Period,
        selectedMonth: YearMonth
    ): List<Expense> {
        val now = LocalDateTime.now()
        val startDateTime = when (period) {
            Period.TODAY -> now.withHour(0).withMinute(0).withSecond(0)
            Period.WEEK -> now.minusWeeks(1)
            Period.CUSTOM_MONTH -> selectedMonth.atDay(1).atStartOfDay()
        }

        val endDateTime = when (period) {
            Period.CUSTOM_MONTH -> selectedMonth.atEndOfMonth().atTime(23, 59, 59)
            else -> now
        }

        return expenses.filter { expense ->
            expense.date >= startDateTime && expense.date <= endDateTime
        }
    }

    fun processSmsExpenses() {
        viewModelScope.launch {
            try {
                Log.e("asd1","Asd");
                expenseRepository.processSmsExpenses(context)
            } catch (e: Exception) {
                // Handle error
                viewModelScope.launch {
                    Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show()
                    Log.e("asd",e.toString());
                }
            }
        }
    }
}