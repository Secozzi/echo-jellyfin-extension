package dev.brahmkshatriya.echo.extension.dto

import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.extension.getImageUrl
import kotlinx.serialization.Serializable

@Serializable
data class AlbumDto(
    val id: String,
    val name: String,
    val imageTags: ImageTagDto,
) : MediaItem {
    override fun toMediaItem(serverUrl: String): EchoMediaItem {
        return EchoMediaItem.Lists.AlbumItem(
            toAlbum(serverUrl),
        )
    }

    fun toAlbum(serverUrl: String): Album {
        return Album(
            id = this.id,
            title = this.name,
            cover = this.imageTags.primary.getImageUrl(serverUrl, this.id),
        )
    }
}
