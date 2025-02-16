package com.example.expensetracker.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expensetracker.ui.theme.DarkBlue
import com.example.expensetracker.ui.theme.FoodColor
import com.example.expensetracker.ui.theme.GroceryColor
import com.example.expensetracker.ui.theme.InvestmentColor
import com.example.expensetracker.ui.theme.MiscColor
import com.example.expensetracker.ui.theme.RechargeColor
import com.example.expensetracker.ui.theme.SoftWhite

@Composable
fun CategoryCard(
    category: String,
    amount: Double,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val categoryColor = when(category.lowercase()) {
        "food" -> FoodColor
        "grocery" -> GroceryColor
        "recharge" -> RechargeColor
        "investment" -> InvestmentColor
        else -> MiscColor
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkBlue
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = CircleShape,
                color = categoryColor.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = category,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(24.dp),
                    tint = categoryColor
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.titleMedium,
                    color = SoftWhite
                )
                Text(
                    text = "₹${amount.toInt()}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = categoryColor
                )
            }
        }
    }
}
