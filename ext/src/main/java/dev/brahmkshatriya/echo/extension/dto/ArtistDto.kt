package dev.brahmkshatriya.echo.extension.dto

import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.extension.getImageUrl
import kotlinx.serialization.Serializable

@Serializable
data class ArtistDto(
    val id: String,
    val name: String,
    val imageTags: ImageTagDto,
    val overview: String? = null,
) : MediaItem {
    override fun toMediaItem(serverUrl: String): EchoMediaItem {
        return EchoMediaItem.Profile.ArtistItem(
            toArtist(serverUrl),
        )
    }

    fun toArtist(serverUrl: String): Artist {
        return Artist(
            id = this.id,
            name = this.name,
            cover = getImageUrl(serverUrl, this.id),
            description = this.overview,
        )
    }
}
