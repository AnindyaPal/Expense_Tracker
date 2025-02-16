package com.example.expensetracker.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "amount") val amount: Double,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "date") val date: LocalDateTime,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "source") val source: String = "SMS",
    @ColumnInfo(name = "merchant_name") val merchantName: String? = null
)