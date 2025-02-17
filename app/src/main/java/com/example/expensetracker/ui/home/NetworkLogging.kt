package com.example.expensetracker.ui.home

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NetworkLoggingInterceptor : Interceptor {
    companion object {
        private const val TAG = "NetworkAPI"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.nanoTime()

        // Log request
        Log.d(TAG, "Request 🚀")
        Log.d(TAG, "URL: ${request.url}")
        Log.d(TAG, "Method: ${request.method}")
        Log.d(TAG, "Headers: ${request.headers}")
        request.body?.let {
            Log.d(TAG, "Body: ${it.toString()}")
        }

        // Execute request
        val response = chain.proceed(request)

        // Calculate request time
        val duration = (System.nanoTime() - startTime) / 1_000_000 // Convert to milliseconds

        // Log response
        Log.d(TAG, "Response 📥 (${duration}ms)")
        Log.d(TAG, "Status Code: ${response.code}")
        Log.d(TAG, "Headers: ${response.headers}")

        // Try to log response body if present
        try {
            val responseBody = response.peekBody(Long.MAX_VALUE).string()
            Log.d(TAG, "Body: $responseBody")
        } catch (e: Exception) {
            Log.e(TAG, "Could not log response body: ${e.message}")
        }

        return response
    }
}
