package com.hustle.feature.demo.data

import com.google.gson.annotations.SerializedName
import com.hustle.feature.demo.domain.DemoItem

data class DemoItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String = "",
    @SerializedName("tag") val tag: String = "Demo",
    @SerializedName(value = "imageUrl", alternate = ["image_url"])
    val imageUrl: String? = null,
)

fun DemoItemDto.toDomain(): DemoItem = DemoItem(
    id = id,
    title = title,
    description = description,
    tag = tag,
    imageUrl = imageUrl,
)
