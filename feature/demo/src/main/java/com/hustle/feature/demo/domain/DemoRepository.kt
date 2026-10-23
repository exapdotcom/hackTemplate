package com.hustle.feature.demo.domain

import com.hustle.core.network.ApiResult

interface DemoRepository {
    suspend fun getItems(): ApiResult<List<DemoItem>>
}
