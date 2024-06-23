package dev.brahmkshatriya.echo.extension.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class ImageTagDto(
    @SerialName("Primary") val primary: String? = null,
)

@Serializable
class ArtistItemsDto(
    @SerialName("Name") val name: String,
    @SerialName("Id") val id: String,
)
