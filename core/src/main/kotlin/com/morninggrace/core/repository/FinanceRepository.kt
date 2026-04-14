package com.morninggrace.core.repository

import com.morninggrace.core.model.FinanceData

interface FinanceRepository {
    suspend fun getSandP500(): FinanceData?
}
