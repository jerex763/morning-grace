package com.morninggrace.core.repository

import com.morninggrace.core.model.FinanceData

interface FinanceRepository {
    suspend fun getMarketData(): List<FinanceData>
}
