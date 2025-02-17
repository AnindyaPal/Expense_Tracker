package com.example.expensetracker.ui.home

import retrofit2.http.Body
import retrofit2.http.POST


interface MutualFundApiService {
    @POST("wp-json/mf-overlap/v1/get-all-mutual-funds")
    suspend fun searchMutualFunds(
        @Body request: Map<String, String>
    ): MutualFundResponse

    @POST("wp-json/mf-overlap/v1/overlap-calculation")
    suspend fun calculateOverlap(
        @Body request: OverlapRequest
    ): OverlapResponse
}