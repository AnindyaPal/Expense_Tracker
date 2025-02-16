package com.example.expensetracker.data.repository

import android.content.Context
import com.example.expensetracker.Expense
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface ExpenseRepository {
    fun getExpensesByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<Expense>>
    fun getTotalExpenseForPeriod(startDate: LocalDateTime, endDate: LocalDateTime): Flow<Double>
    fun getExpensesByCategory(): Flow<Map<String, Double>>
    suspend fun insertExpense(expense: Expense)
    suspend fun processSmsExpenses(context: Context)
}