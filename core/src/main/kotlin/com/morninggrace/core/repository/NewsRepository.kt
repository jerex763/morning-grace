package com.morninggrace.core.repository

import com.morninggrace.core.model.NewsHeadline

interface NewsRepository {
    suspend fun getTopHeadlines(count: Int = 3): List<NewsHeadline>
}
