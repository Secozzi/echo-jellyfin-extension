package dev.brahmkshatriya.echo.extension.models

import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.extension.toImage
import kotlinx.serialization.Serializable

@Serializable
class ArtistDto(
    val name: String,
    val id: String,
    val overview: String? = null,
    val imageTags: ImageTagDto,
) : MediaItem {
    override fun toMediaItem(serverUrl: String): EchoMediaItem {
        return EchoMediaItem.Profile.ArtistItem(
            toArtist(serverUrl),
        )
    }

    fun toArtist(serverUrl: String): Artist {
        return Artist(
            cover = this.imageTags.primary.toImage(serverUrl, this.id),
            description = this.overview,
            id = this.id,
            name = this.name,
        )
    }
}
