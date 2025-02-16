package com.example.expensetracker.data

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.expensetracker.Expense
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.example.expensetracker.ui.theme.DarkBlue
import com.example.expensetracker.ui.theme.FoodColor
import com.example.expensetracker.ui.theme.NeonYellow
import com.example.expensetracker.ui.theme.GroceryColor
import com.example.expensetracker.ui.theme.InvestmentColor
import com.example.expensetracker.ui.theme.MiscColor
import com.example.expensetracker.ui.theme.RechargeColor
import com.example.expensetracker.ui.theme.SoftWhite

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ExpensesList(
    expenses: List<Expense>,
    modifier: Modifier = Modifier,
    showAll: Boolean = false
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = if (showAll) expenses else expenses.take(5),
            key = { it.id }
        ) { expense ->
            ExpenseCard(expense)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ExpenseCard(expense: Expense) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkBlue
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left section with icon and details
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Icon
                Surface(
                    shape = CircleShape,
                    color = getCategoryColor(expense.category).copy(alpha = 0.2f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = getCategoryIcon(expense.category),
                        contentDescription = expense.category,
                        modifier = Modifier.padding(8.dp),
                        tint = getCategoryColor(expense.category)
                    )
                }

                // Merchant and Date
                Column {
                    Text(
                        text = expense.merchantName ?: "Unknown",
                        style = MaterialTheme.typography.titleMedium,
                        color = SoftWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = expense.date.format(DateTimeFormatter.ofPattern("dd MMM, HH:mm")),
                        style = MaterialTheme.typography.bodySmall,
                        color = SoftWhite.copy(alpha = 0.6f)
                    )
                }
            }

            // Amount
            Text(
                text = "₹${expense.amount.toInt()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NeonYellow
            )
        }
    }
}

private fun getCategoryIcon(category: String) = when (category.lowercase()) {
    "food" -> Icons.Default.Favorite
    "grocery" -> Icons.Default.ShoppingCart
    "recharge" -> Icons.Default.Phone
    "investment" -> Icons.Default.Info
    "transportation" -> Icons.Default.LocationOn
    "entertainment" -> Icons.Default.Info
    "health" -> Icons.Default.Info
    "bills" -> Icons.Default.Info
    else -> Icons.Default.Info
}

private fun getCategoryColor(category: String) = when (category.lowercase()) {
    "food" -> FoodColor
    "grocery" -> GroceryColor
    "recharge" -> RechargeColor
    "investment" -> InvestmentColor
    else -> MiscColor
}