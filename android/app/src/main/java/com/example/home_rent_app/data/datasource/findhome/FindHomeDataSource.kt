package com.example.home_rent_app.data.datasource.findhome

import com.example.home_rent_app.data.dto.RoomSearchResultDTO
import kotlinx.coroutines.flow.Flow

interface FindHomeDataSource {

    fun getSearchResult(
        page: Int,
        size: Int,
        availableOnly: Boolean,
        sortedBy: String,
        searchAddress: String
    ): Flow<RoomSearchResultDTO>
}
