package com.example.expensetracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.MapInfo
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getExpensesByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<ExpenseEntity>>

    @Query("SELECT SUM(amount) FROM expenses WHERE date >= :startDate AND :endDate")
    fun getTotalExpenseForPeriod(startDate: LocalDateTime, endDate: LocalDateTime): Flow<Double?>

    @MapInfo(keyColumn = "category", valueColumn = "total")
    @Query("SELECT category, SUM(amount) as total FROM expenses GROUP BY category")
    fun getExpensesByCategory(): Flow<Map<String, Double>>

    // Prevent duplicates by ignoring conflicts
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExpense(expense: ExpenseEntity)

    // Check if an expense with the same details already exists
    @Query("SELECT COUNT(*) FROM expenses WHERE amount = :amount AND date = :date AND category = :category")
    suspend fun checkIfExists(amount: Double, date: LocalDateTime, category: String): Int
}
