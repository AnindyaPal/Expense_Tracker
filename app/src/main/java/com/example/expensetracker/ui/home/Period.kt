package com.example.expensetracker.ui.home

import java.time.YearMonth

enum class Period {
    TODAY,
    WEEK,
    CUSTOM_MONTH
}

data class CustomMonth(
    val yearMonth: YearMonth
)