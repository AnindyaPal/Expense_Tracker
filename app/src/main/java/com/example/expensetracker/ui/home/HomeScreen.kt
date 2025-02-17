package com.example.expensetracker.ui.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expensetracker.data.ExpensesList
import com.example.expensetracker.ui.theme.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    padding: PaddingValues,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val expenses by viewModel.displayedExpenses.collectAsState()
    val categoryTotals by viewModel.expensesByCategory.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedCategoryTotal by viewModel.selectedCategoryTotal.collectAsState()
    val availableMonths by viewModel.availableMonths.collectAsState()
    val periodTotal by viewModel.periodTotal.collectAsState()
    var showAllTransactions by remember { mutableStateOf(false) }
    var showMonthDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .gradientBackground()
            .padding(padding)
    ) {
        // Period Selection Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = DarkBlue
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Overview",
                    style = MaterialTheme.typography.headlineSmall,
                    color = SoftWhite,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Period Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ElevatedFilterChip(
                            selected = selectedPeriod == Period.TODAY,
                            onClick = { viewModel.setPeriod(Period.TODAY) },
                            label = {
                                Text(
                                    "Today",
                                    color = if (selectedPeriod == Period.TODAY) Color.Black else SoftWhite
                                )
                            },
                            colors = FilterChipDefaults.elevatedFilterChipColors(
                                selectedContainerColor = NeonYellow,
                                containerColor = DarkGray,
                                selectedLabelColor = Color.Black
                            ),
                            elevation = FilterChipDefaults.elevatedFilterChipElevation(elevation = 4.dp)
                        )

                        if (selectedPeriod == Period.TODAY) {
                            Text(
                                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM")),
                                style = MaterialTheme.typography.bodySmall,
                                color = SoftWhite.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ElevatedFilterChip(
                            selected = selectedPeriod == Period.WEEK,
                            onClick = { viewModel.setPeriod(Period.WEEK) },
                            label = {
                                Text(
                                    "Week",
                                    color = if (selectedPeriod == Period.WEEK) Color.Black else SoftWhite
                                )
                            },
                            colors = FilterChipDefaults.elevatedFilterChipColors(
                                selectedContainerColor = NeonYellow,
                                containerColor = DarkGray,
                                selectedLabelColor = Color.Black
                            ),
                            elevation = FilterChipDefaults.elevatedFilterChipElevation(elevation = 4.dp)
                        )
                        if (selectedPeriod == Period.WEEK) {
                            val now = LocalDateTime.now()
                            val weekAgo = now.minusWeeks(1)
                            Text(
                                "${weekAgo.format(DateTimeFormatter.ofPattern("dd MMM"))} - ${
                                    now.format(
                                        DateTimeFormatter.ofPattern("dd MMM")
                                    )
                                }",
                                style = MaterialTheme.typography.bodySmall,
                                color = SoftWhite.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Box {
                        ElevatedFilterChip(
                            selected = selectedPeriod == Period.CUSTOM_MONTH,
                            onClick = { showMonthDropdown = true },
                            label = {
                                Text(
                                    selectedMonth.format(DateTimeFormatter.ofPattern("MMM yy")),
                                    color = if (selectedPeriod == Period.CUSTOM_MONTH) Color.Black else SoftWhite
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    "Select month",
                                    tint = if (selectedPeriod == Period.CUSTOM_MONTH) Color.Black else SoftWhite
                                )
                            },
                            colors = FilterChipDefaults.elevatedFilterChipColors(
                                selectedContainerColor = NeonYellow,
                                containerColor = DarkGray,
                                selectedLabelColor = Color.Black  // Add this line
                            ),
                            elevation = FilterChipDefaults.elevatedFilterChipElevation(
                                elevation = 4.dp
                            )
                        )
                        DropdownMenu(
                            expanded = showMonthDropdown,
                            onDismissRequest = { showMonthDropdown = false }
                        ) {
                            availableMonths.forEach { month ->
                                DropdownMenuItem(
                                    text = { Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy"))) },
                                    onClick = {
                                        viewModel.setMonth(month)
                                        showMonthDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Total display
                Text(
                    text = when {
                        selectedCategory != null -> "$selectedCategory: ₹${selectedCategoryTotal?.toInt() ?: 0}"
                        else -> "₹${periodTotal.toInt()}"
                    },
                    style = MaterialTheme.typography.headlineLarge,
                    color = NeonYellow,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        when {
            showAllTransactions -> {
                // All Transactions View
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "All Transactions",
                        style = MaterialTheme.typography.titleLarge,
                        color = SoftWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = { showAllTransactions = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonYellow,
                            contentColor = DeepBlue
                        ),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(
                            "Back",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                ExpensesList(
                    expenses = viewModel.filteredExpenses.collectAsState().value,
                    showAll = true,
                    modifier = Modifier.weight(1f)
                )
            }

            selectedCategory != null -> {
                // Category Transactions View
                var showSortDropdown by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    // Title row
                    Text(
                        text = "$selectedCategory Transactions",
                        style = MaterialTheme.typography.titleLarge,
                        color = SoftWhite,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Controls row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sort dropdown
                        Box {
                            ElevatedFilterChip(
                                selected = false,
                                onClick = { showSortDropdown = true },
                                label = {
                                    Text(
                                        viewModel.sortOrder.collectAsState().value.displayName,
                                        color = SoftWhite
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = "Choose sort order",
                                        tint = SoftWhite
                                    )
                                },
                                colors = FilterChipDefaults.elevatedFilterChipColors(
                                    containerColor = DarkGray
                                ),
                                elevation = FilterChipDefaults.elevatedFilterChipElevation(
                                    elevation = 4.dp
                                )
                            )
                            DropdownMenu(
                                expanded = showSortDropdown,
                                onDismissRequest = { showSortDropdown = false }
                            ) {
                                SortOrder.values().forEach { sortOrder ->
                                    DropdownMenuItem(
                                        text = { Text(sortOrder.displayName) },
                                        onClick = {
                                            viewModel.setSort(sortOrder)
                                            showSortDropdown = false
                                        },
                                        colors = MenuDefaults.itemColors(
                                            textColor = SoftWhite
                                        )
                                    )
                                }
                            }
                        }

                        // Back button
                        Button(
                            onClick = { viewModel.selectCategory(null) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonYellow,
                                contentColor = DeepBlue
                            ),
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .height(40.dp)
                        ) {
                            Text(
                                "Back",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }

                ExpensesList(
                    expenses = expenses,
                    showAll = true,
                    modifier = Modifier.weight(1f)
                )
            }

            else -> {
                // Categories Grid View
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.titleLarge,
                        color = SoftWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = { showAllTransactions = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonYellow,
                            contentColor = DeepBlue
                        ),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(
                            "All",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(getCategoryItems(categoryTotals)) { item ->
                        CategoryCard(
                            category = item.category,
                            amount = item.amount,
                            icon = item.icon,
                            onClick = { viewModel.selectCategory(item.category) }
                        )
                    }
                }
            }
        }
    }
}

private data class CategoryItem(
    val category: String,
    val amount: Double,
    val icon: ImageVector
)

private fun getCategoryItems(categoryTotals: Map<String, Double>): List<CategoryItem> {
    return categoryTotals.map { (category, amount) ->
        CategoryItem(
            category = category,
            amount = amount,
            icon = getCategoryIcon(category)
        )
    }
}

private fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "food" -> Icons.Default.Favorite
        "grocery" -> Icons.Default.ShoppingCart
        "recharge" -> Icons.Default.Phone
        "investment" -> Icons.Default.Star
        "transportation" -> Icons.Default.LocationOn
        "entertainment" -> Icons.Default.Info
        "health" -> Icons.Default.Info
        "bills" -> Icons.Default.Info
        else -> Icons.Default.Info
    }
}