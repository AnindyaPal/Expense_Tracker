package com.example.expensetracker.ui.home

// Base API response models
data class MetaResponse(
    val status: Int,
    val message: String
)

// Mutual Fund Search Models
data class MutualFundResponse(
    val meta: MetaResponse,
    val data: List<MutualFund>
)

data class MutualFund(
    val id: String,
    val sd_scheme_isin: String,
    val fund_name: String
)

// Overlap Calculation Models
data class OverlapRequest(
    val isin: List<String>
)

data class OverlapResponse(
    val meta: MetaResponse,
    val data: OverlapData
)

data class OverlapData(
    val overlap: List<OverlapDetail>,
    val top_securities: List<TopSecurity>
)

data class OverlapDetail(
    val isin1: String,
    val legal_name_isin1: String,
    val isin2: String,
    val legal_name_isin2: String,
    val total_securities_isin1: String,
    val total_securities_isin2: String,
    val overlapped_securities_count: String,
    val overlap_ratio_left: String,
    val overlap_ratio_right: String,
    val pd_holding_min: String
)

data class TopSecurity(
    val company_name: String,
    val combined_weight: Double,
    val list: Map<String, String>
)