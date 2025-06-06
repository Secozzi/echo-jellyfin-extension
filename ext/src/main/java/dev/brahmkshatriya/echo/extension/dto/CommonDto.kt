package dev.brahmkshatriya.echo.extension.dto

import kotlinx.serialization.Serializable

@Serializable
data class ImageTagDto(
    val primary: String? = null,
)

@Serializable
data class ArtistItemDto(
    val id: String,
    val name: String,
)
