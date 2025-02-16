package com.example.expensetracker.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.expensetracker.SmsProcessor
import com.example.expensetracker.data.local.AppDatabase
import com.example.expensetracker.data.local.ExpenseDao
import com.example.expensetracker.data.local.SettingsDao
import com.example.expensetracker.data.repository.ExpenseRepository
import com.example.expensetracker.data.repository.ExpenseRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "expense_tracker_db"  // Changed database name to force new creation
        )
            .fallbackToDestructiveMigration()  // This will recreate tables if schema changes
            .build()
    }

    @Provides
    fun provideExpenseDao(database: AppDatabase): ExpenseDao {
        return database.expenseDao()
    }

    @Provides
    fun provideSettingsDao(database: AppDatabase): SettingsDao {
        return database.settingsDao()
    }

    @Provides
    @Singleton
    fun provideSmsProcessor(settingsDao: SettingsDao): SmsProcessor {
        return SmsProcessor(settingsDao)
    }

    @Provides
    @Singleton
    fun provideExpenseRepository(
        expenseDao: ExpenseDao,
        smsProcessor: SmsProcessor
    ): ExpenseRepository {
        return ExpenseRepositoryImpl(expenseDao, smsProcessor)
    }
}