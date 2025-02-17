@file:OptIn(ExperimentalFoundationApi::class)

package com.example.expensetracker.ui.home

import android.R.attr.text
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expensetracker.ui.theme.DarkBlue
import com.example.expensetracker.ui.theme.DarkGray
import com.example.expensetracker.ui.theme.DeepBlue
import com.example.expensetracker.ui.theme.NeonYellow
import com.example.expensetracker.ui.theme.SoftWhite
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MutualFundScreen(
    padding: PaddingValues,
    viewModel: MutualFundViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = Modifier.padding(padding),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = state.currentTab.ordinal
            ) {
                Tab(
                    selected = state.currentTab == MutualFundTab.SELECTION,
                    onClick = { viewModel.setCurrentTab(MutualFundTab.SELECTION) },
                    text = { Text("Select Funds") }
                )
                Tab(
                    selected = state.currentTab == MutualFundTab.COMPARISON,
                    onClick = {
                        if (state.selectedFunds.size >= 2) {
                            viewModel.compareSelectedFunds()
                        }
                    },
                    text = { Text("Comparison") },
                    enabled = state.selectedFunds.size >= 2
                )
            }

            when (state.currentTab) {
                MutualFundTab.SELECTION -> SelectionScreen(
                    state = state,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onAddFund = { fund ->
                        viewModel.addMutualFund(fund)
                        scope.launch {
                            snackbarHostState.showSnackbar("Added ${fund.fund_name}")
                        }
                    },
                    onRemoveFund = { fund ->
                        viewModel.removeMutualFund(fund)
                        scope.launch {
                            snackbarHostState.showSnackbar("Removed ${fund.fund_name}")
                        }
                    },
                    onCompare = viewModel::compareSelectedFunds
                )

                MutualFundTab.COMPARISON -> ComparisonScreen(
                    state = state
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionScreen(
    state: MutualFundScreenState,
    onSearchQueryChange: (String) -> Unit,
    onAddFund: (MutualFund) -> Unit,
    onRemoveFund: (MutualFund) -> Unit,
    onCompare: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Compare Mutual Funds",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .animateContentSize(),
            placeholder = { Text("Search mutual funds...") },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            singleLine = true
        )

        if (state.isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }

        AnimatedVisibility(
            visible = state.searchHint != null && !state.isLoading && state.searchResults.isEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            state.searchHint?.let { hint ->
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = state.searchResults.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 150.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(
                        items = state.searchResults,
                        key = { it.sd_scheme_isin }
                    ) { fund ->
                        SearchResultItem(
                            fund = fund,
                            onSelect = onAddFund
                        )
                    }
                }
            }
        }

        Text(
            text = "Selected Funds (${state.selectedFunds.size}/5)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(
                items = state.selectedFunds,
                key = { it.sd_scheme_isin }
            ) { fund ->
                SelectedFundItem(
                    fund = fund,
                    onRemove = onRemoveFund
                )
            }
        }

        Button(
            onClick = onCompare,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            enabled = state.selectedFunds.size >= 2,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = when {
                    state.selectedFunds.isEmpty() -> "Add funds to compare"
                    state.selectedFunds.size == 1 -> "Add one more fund"
                    else -> "Compare ${state.selectedFunds.size} Funds"
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultItem(
    fund: MutualFund,
    onSelect: (MutualFund) -> Unit
) {
    Card(
        onClick = { onSelect(fund) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = fund.fund_name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
                Text(
                    text = "ISIN: ${fund.sd_scheme_isin}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SelectedFundItem(
    fund: MutualFund,
    onRemove: (MutualFund) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = fund.fund_name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "ISIN: ${fund.sd_scheme_isin}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            IconButton(
                onClick = { onRemove(fund) }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun ComparisonScreen(
    state: MutualFundScreenState
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            state.overlapResult?.let { result ->
                LazyColumn {
                    item {
                        OverlapResultsSection(result)
                    }
                }
            }
        }
    }
}

@Composable
fun OverlapResultsSection(result: OverlapData?) {
    if (result == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No overlap data available",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Portfolio Overlap Analysis",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Overlap Analysis
            if (!result.overlap.isNullOrEmpty()) {
                result.overlap.forEachIndexed { index, overlap ->
                    Column {
                        LinearOverlap(overlap = overlap)

                        if (index < result.overlap.size - 1) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                thickness = 1.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            } else {
                Text(
                    text = "No overlap information available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            // Top Securities Section
            if (!result.top_securities.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Top Common Holdings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                result.top_securities.forEach { security ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            // Company Name and Combined Weight
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = security.company_name ?: "N/A",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "%.2f%%".format(security.combined_weight ?: 0.0),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Individual Fund Weights
                            security.list?.let { weights ->
                                weights.forEach { (fundName, weight) ->
                                    if (weight != null) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = fundName?.replace("_", " ") ?: "Unknown Fund",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "%.2f%%".format(weight.toDoubleOrNull() ?: 0.0),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LinearOverlap(
    overlap: OverlapDetail,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(DarkBlue) // Use theme's dark blue background
    ) {
        // Two-toned horizontal bar
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepBlue) // Use theme's deep blue for base color
        ) {
            // Left fund's portion
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(DeepBlue) // Use theme's dark gray
            )

            // Overlapping portion
            Box(
                modifier = Modifier
                    .weight(
                        ((overlap.pd_holding_min.toDoubleOrNull() ?: 0.0)
                            .coerceIn(0.0, 100.0) / 100.0f).toFloat()
                    )
                    .fillMaxHeight()
                    .background(NeonYellow) // Use theme's neon yellow for overlap
            )

            // Right fund's remaining portion
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(DarkGray) // Use theme's dark gray
            )
        }

        // Fund names and percentage
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left fund name
                Text(
                    text = overlap.legal_name_isin1,
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftWhite,
                    modifier = Modifier
                        .width(120.dp)
                        .basicMarquee(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Percentage in center
                Text(
                    text = "%.2f%%".format(overlap.pd_holding_min.toDoubleOrNull() ?: 0.0),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeonYellow,
                    fontWeight = FontWeight.Bold
                )

                // Right fund name
                Text(
                    text = overlap.legal_name_isin2,
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftWhite,
                    modifier = Modifier
                        .width(120.dp)
                        .basicMarquee(),
                    maxLines = 1,
                    textAlign = TextAlign.End,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


@Composable
private fun VennDiagramOverlap(
    overlap: OverlapDetail,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.5f)
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Create SVG of Venn diagram
            val svgModifier = Modifier.fillMaxSize()
            Canvas(modifier = svgModifier) {
                val centerY = size.height / 2
                val radius = size.height / 2
                val offset = radius * 0.7f

                // Left circle
                drawCircle(
                    color = Color(0xFF90CAF9),
                    radius = radius,
                    center = Offset(size.width / 2 - offset, centerY),
                    alpha = 0.7f
                )

                // Right circle
                drawCircle(
                    color = Color(0xFFFFCC80),
                    radius = radius,
                    center = Offset(size.width / 2 + offset, centerY),
                    alpha = 0.7f
                )

                // Overlap percentage text
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 48f
                        color = android.graphics.Color.BLACK // Use a default color
                        isFakeBoldText = true
                    }
                    drawText(
                        "%.2f%%".format(overlap.pd_holding_min.toDoubleOrNull() ?: 0.0),
                        size.width / 2,
                        centerY + 16,
                        paint
                    )
                }
            }

            // Fund names
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Color(0xFF90CAF9), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = overlap.legal_name_isin1,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Color(0xFFFFCC80), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = overlap.legal_name_isin2,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}