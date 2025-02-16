package com.example.expensetracker

import com.example.expensetracker.data.local.ExpenseEntity
import java.time.LocalDateTime

data class Expense(
    val id: Long = 0,
    val amount: Double,
    val category: String,
    val date: LocalDateTime,
    val description: String,
    val source: String = "SMS",
    val merchantName: String? = null
)

fun ExpenseEntity.toDomain() = Expense(
    id = id,
    amount = amount,
    category = category,
    date = date,
    description = description,
    source = source,
    merchantName = merchantName
)

fun Expense.toEntity() = ExpenseEntity(
    id = id,
    amount = amount,
    category = category,
    date = date,
    description = description,
    source = source,
    merchantName = merchantName
)