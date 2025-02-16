package com.example.expensetracker.data.repository

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.expensetracker.Expense
import com.example.expensetracker.SmsProcessor
import com.example.expensetracker.data.local.ExpenseDao
import com.example.expensetracker.toDomain
import com.example.expensetracker.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject

class ExpenseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val smsProcessor: SmsProcessor
) : ExpenseRepository {
    override fun getExpensesByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<Expense>> =
        expenseDao.getExpensesByDateRange(startDate, endDate).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getTotalExpenseForPeriod(startDate: LocalDateTime, endDate: LocalDateTime): Flow<Double> =
        expenseDao.getTotalExpenseForPeriod(startDate, endDate).map { it ?: 0.0 }

    override fun getExpensesByCategory(): Flow<Map<String, Double>> =
        expenseDao.getExpensesByCategory()

    override suspend fun insertExpense(expense: Expense) {
        expenseDao.insertExpense(expense.toEntity())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun processSmsExpenses(context: Context) {
        val expenses = smsProcessor.extractExpenses(context)
        expenses.forEach { insertExpense(it) }
    }
}