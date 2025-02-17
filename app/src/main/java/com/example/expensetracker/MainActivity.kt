package com.example.expensetracker

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.expensetracker.ui.home.HomeScreen
import com.example.expensetracker.ui.home.HomeViewModel
import com.example.expensetracker.ui.home.MutualFundScreen
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: HomeViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.O)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.processSmsExpenses()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestSmsPermission()
        setContent {
            ExpenseTrackerTheme {
                HomeNavigator(viewModel)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun requestSmsPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED -> {
                viewModel.processSmsExpenses()
            }
            else -> {
                requestPermissionLauncher.launch(android.Manifest.permission.READ_SMS)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeNavigator(viewModel: HomeViewModel) {
    var selectedTab by remember { mutableStateOf(1) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf(
                    Triple(Icons.Default.Home, "Mutual Funds", 0),
                    Triple(Icons.Default.Home, "Expenses", 1),
                    Triple(Icons.Default.Home, "AI Assistant", 2)
                ).forEach { (icon, label, index) ->
                    NavigationBarItem(
                        icon = { Icon(icon, label) },
                        label = { Text(label) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
           // 0 -> NetWorthScreen(padding)
            0 -> MutualFundScreen(padding)
            1 -> HomeScreen(padding, viewModel)
            2 -> ChatScreen(padding)
        }
    }

}

