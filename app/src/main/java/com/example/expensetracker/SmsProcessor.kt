package com.example.expensetracker

import android.Manifest
import android.content.pm.PackageManager
import android.content.Context
import androidx.core.content.ContextCompat
import java.time.LocalDateTime
import java.time.Instant
import java.time.ZoneId
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.expensetracker.data.local.SettingsDao
import com.example.expensetracker.data.local.SettingsEntity
import javax.inject.Inject

class SmsProcessor @Inject constructor(    private val settingsDao: SettingsDao
) {

    companion object {
        const val CATEGORY_FOOD = "Food"
        const val CATEGORY_GROCERY = "Grocery"
        const val CATEGORY_RECHARGE = "Recharge"
        const val CATEGORY_MISC = "Misc"
        const val CATEGORY_INVESTMENT = "Investment"
        const val LAST_SMS_SYNC_KEY = "last_sms_sync"

        // Case-sensitive exact merchant matches
        private val merchantCategories = listOf(
            // Food delivery
            MerchantCategory("Swiggy Limited", CATEGORY_FOOD),      // Food delivery
            MerchantCategory("BUNDL TECHNOLOGIES", CATEGORY_FOOD),
            MerchantCategory("Bundl Technologi", CATEGORY_FOOD),
            MerchantCategory("ZOMATO", CATEGORY_FOOD),
            // Groceries
            MerchantCategory("BLINKIT", CATEGORY_GROCERY),
            MerchantCategory("ZEPTO", CATEGORY_GROCERY),
            MerchantCategory("BIGBASKET", CATEGORY_GROCERY),
            MerchantCategory("SWIGGY LIMITED", CATEGORY_GROCERY),    // Instamart
            // Investment platforms with variations
            MerchantCategory("ANGEL LTD NSE", CATEGORY_INVESTMENT),
            MerchantCategory("ANGEL ONE", CATEGORY_INVESTMENT),
            MerchantCategory("ANGEL One Limite", CATEGORY_INVESTMENT),
            MerchantCategory("ANGEL BROKING", CATEGORY_INVESTMENT),
            MerchantCategory("Zerodha Broking", CATEGORY_INVESTMENT),
            MerchantCategory("ZERODHA", CATEGORY_INVESTMENT),
            MerchantCategory("GROWW", CATEGORY_INVESTMENT),
            MerchantCategory("Groww Payments", CATEGORY_INVESTMENT),
            MerchantCategory("GROWW INVESTMENT", CATEGORY_INVESTMENT),
        )

        private val categoryKeywords = mapOf(
            CATEGORY_FOOD to listOf(
                "restaurant", "food", "cafe", "dining"
            ),
            CATEGORY_GROCERY to listOf(
                "supermarket", "grocery", "mart", "store"
            ),
            CATEGORY_RECHARGE to listOf(
                "airtel", "jio", "vi ", "vodafone", "recharge"
            ),
            CATEGORY_INVESTMENT to listOf(
                "mutual fund", "stocks", "investment", "trading", "groww",
                "angel", "zerodha", "nse", "bse", "securities", "broking"
            )
        )
    }

    private data class MerchantCategory(
        val merchantName: String,
        val category: String
    )

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun extractExpenses(context: Context): List<Expense> {
        val lastSync = settingsDao.getSetting(LAST_SMS_SYNC_KEY)?.longValue ?: 0L
        val currentTime = System.currentTimeMillis()

        val processedRefNos = mutableSetOf<String>()
        val expenses = mutableListOf<Expense>()

        val smsUri = getAccessibleSmsUri(context) ?: return emptyList()

        val cursor = context.contentResolver.query(
            smsUri,
            arrayOf("body", "date"),
            "date > ?",  // Only get messages after last sync
            arrayOf(lastSync.toString()),
            "date DESC"
        )

        cursor?.use {
            val bodyIndex = it.getColumnIndex("body")
            val dateIndex = it.getColumnIndex("date")

            while (it.moveToNext()) {
                val body = it.getString(bodyIndex)
                val date = it.getLong(dateIndex)

                val refNo = extractRefNo(body)
                if (refNo != null && !processedRefNos.contains(refNo)) {
                    parseExpenseFromSms(body, date)?.let { expense ->
                        expenses.add(expense)
                        processedRefNos.add(refNo)
                    }
                }
            }
        }

        // Update last sync time
        settingsDao.insertSetting(
            SettingsEntity(
            key = LAST_SMS_SYNC_KEY,
            longValue = currentTime
        )
        )

        return expenses
    }

    private fun extractRefNo(smsBody: String): String? {
        val refNoPattern = "(?i)Refno\\s*(\\d+)"
        return refNoPattern.toRegex().find(smsBody)?.groupValues?.get(1)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun parseExpenseFromSms(body: String, timestamp: Long): Expense? {
        val amountPattern = "(?i)debited by\\s*(\\d+(?:\\.\\d{2})?)"
        val merchantPattern = "(?i)trf to\\s+([^\\s](?:.*?)?)\\s+Refno"

        val amountMatch = amountPattern.toRegex().find(body)
        val merchantMatch = merchantPattern.toRegex().find(body)

        return if (amountMatch != null) {
            val amount = amountMatch.groupValues[1].toDouble()
            val merchantName = merchantMatch?.groupValues?.get(1)?.trim() ?: "Unknown"

            Expense(
                amount = amount,
                category = detectCategory(merchantName, body),
                date = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(timestamp),
                    ZoneId.systemDefault()
                ),
                description = body,
                merchantName = merchantName
            )
        } else null
    }

    private fun detectCategory(merchantName: String, smsBody: String): String {
        // First try exact merchant match
        merchantCategories.find { it.merchantName == merchantName }?.let {
            return it.category
        }

        // Try case-insensitive merchant match
        merchantCategories.find {
            it.merchantName.equals(merchantName, ignoreCase = true)
        }?.let {
            return it.category
        }

        // If no merchant match, check keywords
        val textToCheck = "${merchantName.lowercase()} ${smsBody.lowercase()}"
        for ((category, keywords) in categoryKeywords) {
            if (keywords.any { textToCheck.contains(it) }) {
                return category
            }
        }

        return CATEGORY_MISC
    }

    private fun getAccessibleSmsUri(context: Context): Uri? {
        return try {
            val uri = Uri.parse("content://sms/inbox")
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.count >= 0) return uri
            }
            Uri.parse("content://sms")
        } catch (e: Exception) {
            Uri.parse("content://sms")
        }
    }
}