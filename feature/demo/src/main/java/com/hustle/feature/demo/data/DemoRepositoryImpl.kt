package com.hustle.feature.demo.data

import com.hustle.core.network.ApiResult
import com.hustle.core.network.map
import com.hustle.core.network.safeApiCall
import com.hustle.feature.demo.domain.DemoItem
import com.hustle.feature.demo.domain.DemoRepository
import javax.inject.Inject

class DemoRepositoryImpl @Inject constructor(
    private val api: DemoApi,
) : DemoRepository {

    override suspend fun getItems(): ApiResult<List<DemoItem>> =
        safeApiCall { api.getItems() }
            .map { items -> items.map(DemoItemDto::toDomain) }
}
