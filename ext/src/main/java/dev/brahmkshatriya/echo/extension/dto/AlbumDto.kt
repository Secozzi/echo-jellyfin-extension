package dev.brahmkshatriya.echo.extension.dto

import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.extension.getImageUrl
import kotlinx.serialization.Serializable

@Serializable
data class AlbumDto(
    val id: String,
    val name: String,
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
            cover = getImageUrl(serverUrl, this.id),
        )
    }
}
