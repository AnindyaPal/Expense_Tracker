package com.example.expensetracker.ui.home

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MutualFundStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "mutual_funds_prefs",
        Context.MODE_PRIVATE
    )
    private val gson = Gson()

    fun saveSelectedFunds(funds: List<MutualFund>) {
        val json = gson.toJson(funds)
        prefs.edit().putString(SELECTED_FUNDS_KEY, json).apply()
    }

    fun getSelectedFunds(): List<MutualFund> {
        val json = prefs.getString(SELECTED_FUNDS_KEY, null)
        return if (json != null) {
            val type = object : TypeToken<List<MutualFund>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    companion object {
        private const val SELECTED_FUNDS_KEY = "selected_funds"
    }
}