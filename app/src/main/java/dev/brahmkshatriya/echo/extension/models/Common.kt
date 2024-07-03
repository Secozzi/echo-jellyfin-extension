package dev.brahmkshatriya.echo.extension.models

import kotlinx.serialization.Serializable

@Serializable
class ImageTagDto(
    val primary: String? = null,
)

@Serializable
class ArtistItemsDto(
    val id: String,
    val name: String,
)
