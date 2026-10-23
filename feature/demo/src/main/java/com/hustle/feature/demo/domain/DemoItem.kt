package com.hustle.feature.demo.domain

data class DemoItem(
    val id: String,
    val title: String,
    val description: String,
    val tag: String,
    val imageUrl: String? = null,
)
