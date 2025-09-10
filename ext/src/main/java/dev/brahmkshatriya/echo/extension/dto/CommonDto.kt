package dev.brahmkshatriya.echo.extension.dto

import kotlinx.serialization.Serializable

@Serializable
data class ImageTagDto(
    val primary: String? = null,
    val banner: String? = null,
)

@Serializable
data class ArtistItemDto(
    val id: String,
    val name: String,
)

@Serializable
data class CommonItemDto(
    val userData: UserData,
)

@Serializable
data class UserData(
    val isFavorite: Boolean? = null,
    val playCount: Long? = null,
    val playbackPositionTicks: Long? = null,
)

@Serializable
data class IdDto(
    val id: String,
)
