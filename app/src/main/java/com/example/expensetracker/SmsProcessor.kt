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
import java.util.regex.Pattern
import javax.inject.Inject

class SmsProcessor @Inject constructor(
    private val settingsDao: SettingsDao
) {

    companion object {
        const val CATEGORY_FOOD = "Food"
        const val CATEGORY_GROCERY = "Grocery"
        const val CATEGORY_RECHARGE = "Recharge"
        const val CATEGORY_MISC = "Misc"
        const val CATEGORY_INVESTMENT = "Investment"
        const val CATEGORY_TELECOM = "Telecom"
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
            MerchantCategory("ICCL-Groww", CATEGORY_INVESTMENT),
            // Telecom
            MerchantCategory("JIO PLATFORMS L", CATEGORY_TELECOM),
            MerchantCategory("AIRTEL", CATEGORY_TELECOM),
            MerchantCategory("BHARTI AIRTEL", CATEGORY_TELECOM),
            MerchantCategory("VI", CATEGORY_TELECOM),
            MerchantCategory("VODAFONE IDEA", CATEGORY_TELECOM)
        )

        private val categoryKeywords = mapOf(
            CATEGORY_FOOD to listOf(
                "restaurant", "food", "cafe", "dining", "swiggy", "zomato"
            ),
            CATEGORY_GROCERY to listOf(
                "supermarket", "grocery", "mart", "store", "blinkit", "zepto", "bigbasket"
            ),
            CATEGORY_RECHARGE to listOf(
                "airtel", "jio", "vi ", "vodafone", "recharge", "mobile bill"
            ),
            CATEGORY_INVESTMENT to listOf(
                "mutual fund", "stocks", "investment", "trading", "groww",
                "angel", "zerodha", "nse", "bse", "securities", "broking", "iccl"
            ),
            CATEGORY_TELECOM to listOf(
                "platforms", "telecom", "jio", "airtel", "vi", "vodafone"
            )
        )

        // Bank identifiers to help with SMS recognition
        private val bankIdentifiers = listOf(
            "ICICI", "SBI", "HDFC", "AXIS", "KOTAK", "YES BANK", "PNB"
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

        val processedTransactions = mutableSetOf<String>()
        val expenses = mutableListOf<Expense>()

        val smsUri = getAccessibleSmsUri(context) ?: return emptyList()

        // Use query with DATE constraint to ensure we only process messages after last sync
        val cursor = context.contentResolver.query(
            smsUri,
            arrayOf("body", "date", "address"),
            "date > ?",  // Only get messages after last sync
            arrayOf(lastSync.toString()),
            "date DESC"
        )

        cursor?.use {
            val bodyIndex = it.getColumnIndex("body")
            val dateIndex = it.getColumnIndex("date")
            val addressIndex = if (it.getColumnIndex("address") >= 0) it.getColumnIndex("address") else -1

            while (it.moveToNext()) {
                val body = it.getString(bodyIndex)
                val date = it.getLong(dateIndex)
                val sender = if (addressIndex >= 0) it.getString(addressIndex) else ""

                // Skip if not likely an expense message
                if (!isLikelyExpenseSms(body)) continue

                // Try to extract expense information
                val expense = parseExpenseFromSms(body, date)

                if (expense != null) {
                    // Create a transaction ID for deduplication from:
                    // 1. Extracted transaction ID if available
                    // 2. Or a combination of amount, date and message hash
                    val transactionId = extractTransactionId(body) ?:
                    "${expense.amount}_${expense.date.toLocalDate()}_${body.hashCode()}"

                    if (!processedTransactions.contains(transactionId)) {
                        expenses.add(expense)
                        processedTransactions.add(transactionId)
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

    /**
     * Carefully check if an SMS is actually an expense transaction (not billing reminders or offers).
     * This avoids capturing bill reminders, marketing messages, NAV updates, or balance info as expenses.
     */
    private fun isLikelyExpenseSms(smsBody: String): Boolean {
        val lowerBody = smsBody.lowercase()

        // REJECT: Bill reminders and payment requests (not actual expenses)
        if ((lowerBody.contains("bill") || lowerBody.contains("due") || lowerBody.contains("payable")) &&
            (lowerBody.contains("due date") || lowerBody.contains("overdue") ||
                    lowerBody.contains("please pay") || lowerBody.contains("please make") ||
                    lowerBody.contains("amount payable") || lowerBody.contains("bill payment"))) {
            return false
        }

        // REJECT: Marketing/promotional messages
        if (lowerBody.contains("offer") ||
            lowerBody.contains("loan") ||
            lowerBody.contains("apply") ||
            lowerBody.contains("qualify") ||
            lowerBody.contains("eligib") ||
            lowerBody.contains("get personal") ||
            lowerBody.contains("chance to") ||
            lowerBody.contains("reward") ||
            lowerBody.contains("cashback") ||
            lowerBody.contains("discount")) {
            return false
        }

        // REJECT: Mutual fund NAV and investment updates
        if ((lowerBody.contains("nav") || lowerBody.contains("mutual fund")) &&
            !lowerBody.contains("debited from") && !lowerBody.contains("spent using")) {
            return false
        }

        // REJECT: Investment portfolio updates
        if (lowerBody.contains("portfolio value") ||
            lowerBody.contains("sip") ||
            lowerBody.contains("folio")) {
            return false
        }

        // ACCEPT: Strong positive indicators that this is an actual transaction
        // These are phrases that almost always indicate an actual expense that has occurred
        val strongTransactionIndicators = listOf(
            "debited from", "debited by", "spent using", "paid using", "withdrawn from",
            "deducted from", "trf to", "transfer to", "txn"
        )

        for (indicator in strongTransactionIndicators) {
            if (lowerBody.contains(indicator)) {
                // Check if this is just a transaction notification that doesn't involve money moving out
                if ((lowerBody.contains("credited") || lowerBody.contains("received")) &&
                    !lowerBody.contains("debited") && !lowerBody.contains("spent") &&
                    !lowerBody.contains("paid") && !lowerBody.contains("deducted")) {
                    return false
                }
                return true
            }
        }

        // REJECT: For balance-only messages, reject them
        if ((lowerBody.contains("balance") || lowerBody.contains("bal")) &&
            !lowerBody.contains("debited") && !lowerBody.contains("spent") &&
            !lowerBody.contains("paid") && !lowerBody.contains("withdrawn")) {
            return false
        }

        // For everything else, require BOTH a clear transaction verb AND a currency amount
        val transactionVerbs = listOf(
            "debited", "spent", "paid", "charged", "purchased",
            "payment", "transferred", "withdrawn"
        )

        // Must have one of these exact verbs (not just containing them)
        val hasTransactionVerb = transactionVerbs.any { verb ->
            "\\b$verb\\b".toRegex(RegexOption.IGNORE_CASE).containsMatchIn(lowerBody)
        }

        // Currency pattern with mandatory rupee symbol and digits
        val currencyPattern = "(?i)(rs\\.?|inr|₹)\\s*(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{1,2})?|\\d+(?:\\.\\d{1,2})?)"
        val hasCurrencyAmount = Pattern.compile(currencyPattern).matcher(smsBody).find()

        // Must have BOTH a transaction verb AND a currency amount with no bill reminder terms
        return hasTransactionVerb && hasCurrencyAmount &&
                !lowerBody.contains("amount payable") && !lowerBody.contains("bill payment")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun parseExpenseFromSms(body: String, timestamp: Long): Expense? {
        // 1. First, extract the transaction amount
        val amount = extractAmount(body) ?: return null

        // 2. Then, try to extract the merchant name
        val merchant = extractMerchant(body)

        // 3. Finally, extract an optional transaction ID (for deduplication)
        val transactionId = extractTransactionId(body)

        // 4. Create the Expense object
        return Expense(
            amount = amount,
            category = detectCategory(merchant, body),
            date = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault()
            ),
            description = body,
            merchantName = merchant
        )
    }

    /**
     * Extract the amount from an SMS message.
     * Handles various formats including commas and different currency notations.
     * Always extracts the amount that was actually spent (not loan offers or available balances).
     */
    private fun extractAmount(body: String): Double? {
        val lowerBody = body.lowercase()

        // First, check if this is a bill reminder, not an actual expense
        if ((lowerBody.contains("amount payable") || lowerBody.contains("bill payment") ||
                    lowerBody.contains("due amount") || lowerBody.contains("total amount payable")) &&
            !lowerBody.contains("debited") && !lowerBody.contains("spent")) {
            return null  // This is a bill reminder, not an expense
        }

        // Create a more universal approach - look for number patterns near transaction indicators
        val transactionIndicators = listOf(
            "debited", "spent", "paid", "withdrawn", "deducted", "transferred",
            "transaction", "purchase", "payment"
        )

        // First check if any transaction indicators exist in the message
        val hasTransactionIndicator = transactionIndicators.any { indicator ->
            lowerBody.contains(indicator)
        }

        if (hasTransactionIndicator) {
            // Find all currency-amount patterns in the message
            val amountPatterns = listOf(
                // Case 1: Currency symbol followed by amount (INR 100.00)
                "(?i)(rs\\.?|inr|₹)\\s*(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{1,2})?|\\d+(?:\\.\\d{1,2})?)",

                // Case 2: Amount followed by currency symbol (100.00 INR)
                "(?i)(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{1,2})?|\\d+(?:\\.\\d{1,2})?)\\s*(?:rs\\.?|inr|₹)",

                // Case 3: Just an amount with decimal point near transaction indicator (100.00)
                "(?i)(\\d{1,3}(?:,\\d{3})*\\.\\d{1,2}|\\d+\\.\\d{1,2})"
            )

            // Store all found amounts with their positions in text
            val foundAmounts = mutableListOf<Pair<Double, Int>>()

            // Find all amounts in the text
            for (pattern in amountPatterns) {
                val matcher = Pattern.compile(pattern).matcher(body)
                while (matcher.find()) {
                    try {
                        // Extract amount group (group 2 for Case 1, group 1 for others)
                        val amountGroup = if (pattern.startsWith("(?i)(rs")) 2 else 1
                        val amountStr = matcher.group(amountGroup)?.trim() ?: continue
                        val cleanAmount = amountStr.replace(",", "")
                        val amount = cleanAmount.toDouble()

                        // Skip if this seems to be an available balance or limit
                        val nearbyText = try {
                            val start = Math.max(0, matcher.start() - 20)
                            val end = Math.min(body.length, matcher.end() + 20)
                            body.substring(start, end).lowercase()
                        } catch (e: Exception) {
                            ""
                        }

                        if (nearbyText.contains("avl") || nearbyText.contains("available") ||
                            nearbyText.contains("limit") || nearbyText.contains("balance") ||
                            nearbyText.contains("bal:")) {
                            continue
                        }

                        foundAmounts.add(Pair(amount, matcher.start()))
                    } catch (e: NumberFormatException) {
                        continue
                    }
                }
            }

            // If we found amounts, find the one closest to a transaction indicator
            if (foundAmounts.isNotEmpty()) {
                val indicatorPositions = mutableListOf<Int>()

                // Find all positions of transaction indicators
                for (indicator in transactionIndicators) {
                    var index = lowerBody.indexOf(indicator)
                    while (index >= 0) {
                        indicatorPositions.add(index)
                        index = lowerBody.indexOf(indicator, index + 1)
                    }
                }

                // If we found transaction indicators, find the amount closest to any indicator
                if (indicatorPositions.isNotEmpty()) {
                    return foundAmounts.minByOrNull { amountPair ->
                        indicatorPositions.minOf { position ->
                            Math.abs(amountPair.second - position)
                        }
                    }?.first
                } else {
                    // Otherwise just return the first amount
                    return foundAmounts.first().first
                }
            }
        }

        // If we've reached here, we couldn't find a valid amount
        return null
    }

    /**
     * Extract the merchant name from an SMS message.
     * Uses multiple patterns to identify merchants in different message formats.
     */
    private fun extractMerchant(body: String): String {
        // Extract financial institution first (for bank transactions)
        val financialInstitution = extractFinancialInstitution(body)
        if (financialInstitution != null) {
            return financialInstitution
        }

        // 1. Try card transaction format: "on DATE on MERCHANT"
        val creditCardPattern = "(?i)on\\s+(\\d{1,2}-\\w{3}-\\d{2,4})\\s+on\\s+([^.\\s]+(?:\\s+[^.\\s]+)*)"
        val creditCardMatcher = Pattern.compile(creditCardPattern).matcher(body)
        if (creditCardMatcher.find()) {
            val merchant = creditCardMatcher.group(2)?.trim()
            if (merchant != null && merchant.length > 2) {
                return cleanMerchantName(merchant)
            }
        }

        // 2. Try "trf to MERCHANT" pattern (common in UPI and bank transfers)
        val trfPattern = "(?i)trf\\s+to\\s+([^.\\s]+(?:\\s+[^.\\s]+)*)"
        val trfMatcher = Pattern.compile(trfPattern).matcher(body)
        if (trfMatcher.find()) {
            val merchant = trfMatcher.group(1)?.trim()
            if (merchant != null && merchant.length > 2 && !isGenericTerm(merchant)) {
                return cleanMerchantName(merchant)
            }
        }

        // 3. Try direct merchant indicators with strong contextual markers
        val directPatterns = listOf(
            "(?i)paid\\s+to\\s+([^.\\s]+(?:\\s+[^.\\s]+)*)",                  // paid to MERCHANT
            "(?i)payment\\s+to\\s+([^.\\s]+(?:\\s+[^.\\s]+)*)",               // payment to MERCHANT
            "(?i)transferred\\s+to\\s+([^.\\s]+(?:\\s+[^.\\s]+)*)",           // transferred to MERCHANT
            "(?i)purchase\\s+(?:at|from)\\s+([^.\\s]+(?:\\s+[^.\\s]+)*)",     // purchase at MERCHANT
            "(?i)buying\\s+from\\s+([^.\\s]+(?:\\s+[^.\\s]+)*)",              // buying from MERCHANT
            "(?i)transaction\\s+at\\s+([^.\\s]+(?:\\s+[^.\\s]+)*)"            // transaction at MERCHANT
        )

        for (pattern in directPatterns) {
            val matcher = Pattern.compile(pattern).matcher(body)
            if (matcher.find()) {
                val merchant = matcher.group(1)?.trim()
                if (merchant != null && merchant.length > 2 && !isGenericTerm(merchant)) {
                    return cleanMerchantName(merchant)
                }
            }
        }

        // 4. Look for known merchants from our catalog
        for (merchantCategory in merchantCategories) {
            if (body.contains(merchantCategory.merchantName, ignoreCase = true)) {
                return merchantCategory.merchantName
            }
        }

        // 5. Look for ALL CAPS words that are likely merchants
        val capsPattern = "\\b([A-Z]{2,}(?:\\s+[A-Z]+)*)\\b"
        val capsMatcher = Pattern.compile(capsPattern).matcher(body)

        val allCapsMatches = mutableListOf<String>()
        while (capsMatcher.find()) {
            val match = capsMatcher.group(1)

            // Skip excluded terms and ensure it's not too short or too long
            val excluded = listOf(
                "INR", "SMS", "RS", "ID", "BLOCK", "CALL", "ALERT", "INFO",
                "BALANCE", "LIMIT", "AVL", "TXN", "FOR", "ANY", "DISCREPANCY", "DIAL",
                "A/C", "ACCOUNT", "DATE", "REF", "DEAR", "USER", "CUSTOMER", "REFNO"
            ) + bankIdentifiers

            if (match != null && !excluded.contains(match) && match.length > 2 && match.length < 25) {
                allCapsMatches.add(match)
            }
        }

        // Return the longest ALL CAPS match (likely to be a merchant name)
        if (allCapsMatches.isNotEmpty()) {
            return allCapsMatches.maxByOrNull { it.length } ?: "Unknown"
        }

        // 6. For specific banks/payment services, use them as merchant when no other merchant is found
        val bankPattern = "(?i)(Airtel\\s+Payments\\s+Bank|ICICI\\s+Bank|SBI|HDFC\\s+Bank|Axis\\s+Bank|Kotak|Yes\\s+Bank|UPI|IMPS)"
        val bankMatcher = Pattern.compile(bankPattern).matcher(body)
        if (bankMatcher.find()) {
            return bankMatcher.group(1) ?: "Unknown"
        }

        return "Unknown"
    }

    /**
     * Extract financial institution from transaction messages where no merchant is specified.
     * Used for bank transfers, cash withdrawals, etc.
     */
    private fun extractFinancialInstitution(body: String): String? {
        val lowerBody = body.lowercase()

        // Check if this is a transaction that specifies a financial institution but not necessarily a merchant
        val isGenericTransaction = lowerBody.contains("debited from") ||
                lowerBody.contains("withdrawn from") ||
                lowerBody.contains("transaction id") ||
                lowerBody.contains("txn id")

        if (isGenericTransaction) {
            // Check for bank/payment app names
            val bankPatterns = listOf(
                "(?i)(Airtel\\s+Payments\\s+Bank)\\s+a\\/c",  // Airtel Payments Bank a/c
                "(?i)(ICICI\\s+Bank)\\s+(?:a\\/c|account|card)",  // ICICI Bank account
                "(?i)(SBI)\\s+(?:a\\/c|account|card)",  // SBI account
                "(?i)(HDFC\\s+Bank)\\s+(?:a\\/c|account|card)",  // HDFC Bank account
                "(?i)(Axis\\s+Bank)\\s+(?:a\\/c|account|card)",  // Axis Bank account
                "(?i)(Kotak)\\s+(?:a\\/c|account|card)",  // Kotak account
                "(?i)(Yes\\s+Bank)\\s+(?:a\\/c|account|card)",  // Yes Bank account
                "(?i)from\\s+(\\w+\\s+(?:Bank|Payments))\\s",  // from [Bank Name]
                "(?i)using\\s+(\\w+\\s+Bank)\\s"  // using [Bank Name]
            )

            for (pattern in bankPatterns) {
                val matcher = Pattern.compile(pattern).matcher(body)
                if (matcher.find()) {
                    return matcher.group(1)
                }
            }

            // For payment apps
            val paymentAppPatterns = listOf(
                "(?i)(Paytm)\\s+wallet",  // Paytm wallet
                "(?i)(GooglePay|GPay)",   // GooglePay
                "(?i)(PhonePe)",          // PhonePe
                "(?i)(Amazon\\s+Pay)",    // Amazon Pay
                "(?i)(MobiKwik)"          // MobiKwik
            )

            for (pattern in paymentAppPatterns) {
                val matcher = Pattern.compile(pattern).matcher(body)
                if (matcher.find()) {
                    return matcher.group(1)
                }
            }
        }

        return null
    }

    /**
     * Check if a term is too generic to be a merchant
     */
    private fun isGenericTerm(term: String): Boolean {
        val genericTerms = listOf(
            "account", "bank", "card", "credit", "debit", "transaction",
            "payment", "transfer", "atm", "cash", "money", "fund", "wallet",
            "a/c", "balance", "discrepancy", "dial", "alert", "info", "sms"
        )

        return genericTerms.any {
            term.lowercase().contains(it) && term.length < 12
        }
    }

    /**
     * Clean up extracted merchant name by trimming punctuation and extra spaces.
     */
    private fun cleanMerchantName(name: String?): String {
        if (name == null) return "Unknown"

        // Remove common terminators like periods, commas
        var cleaned = name.trim()
            .replace(Regex("[.,;:]$"), "")
            .trim()

        // Remove "Ref" or "Reference" if it appears at the end
        cleaned = cleaned.replace(Regex("(?i)\\s+Ref\\s*$"), "")
            .replace(Regex("(?i)\\s+Reference\\s*$"), "")
            .replace(Regex("(?i)\\s+Refno\\s*.*$"), "")
            .trim()

        // Remove non-merchant terms
        val nonMerchantTerms = listOf("for any", "dial", "discrepancy", "avl limit")
        for (term in nonMerchantTerms) {
            if (cleaned.lowercase().contains(term)) {
                cleaned = cleaned.substring(0, cleaned.lowercase().indexOf(term)).trim()
            }
        }

        return if (cleaned.isBlank()) "Unknown" else cleaned
    }

    /**
     * Extract a transaction ID from an SMS message for deduplication.
     */
    private fun extractTransactionId(body: String): String? {
        // Common transaction ID patterns
        val transactionIdPatterns = listOf(
            "(?i)Txn\\s*ID\\s*([A-Za-z0-9]+)",                  // Txn ID 12345
            "(?i)Txn\\s*No\\s*([A-Za-z0-9]+)",                  // Txn No 12345
            "(?i)Transaction\\s*ID\\s*([A-Za-z0-9]+)",          // Transaction ID 12345
            "(?i)Transaction\\s*No\\s*([A-Za-z0-9]+)",          // Transaction No 12345
            "(?i)Ref\\s*No\\s*([A-Za-z0-9]+)",                  // Ref No 12345
            "(?i)Reference\\s*No\\s*([A-Za-z0-9]+)",            // Reference No 12345
            "(?i)Refno\\s*(\\d+)",                              // Refno 12345
            "(?i)Card\\s*XX(\\d{4})",                           // Card XX1234
            "(?i)IMPS/P2A/(\\w+)",                              // IMPS/P2A/ABCDEF
            "(?i)UPI\\s*Ref\\s*([A-Za-z0-9]+)"                  // UPI Ref 12345
        )

        for (pattern in transactionIdPatterns) {
            val matcher = Pattern.compile(pattern).matcher(body)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }

        return null
    }

    /**
     * Determine the expense category based on merchant name and message content.
     */
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

    /**
     * Get a valid SMS URI from the content provider.
     */
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