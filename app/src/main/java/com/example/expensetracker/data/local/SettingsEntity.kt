package com.example.expensetracker.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey
    @ColumnInfo(name = "key")
    val key: String,

    @ColumnInfo(name = "longValue")
    val longValue: Long? = null,

    @ColumnInfo(name = "stringValue")
    val stringValue: String? = null
)