package com.hustle.feature.demo.data

import retrofit2.http.GET

interface DemoApi {
    @GET("demo/items")
    suspend fun getItems(): List<DemoItemDto>
}
