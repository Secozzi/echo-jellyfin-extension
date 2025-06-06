package dev.brahmkshatriya.echo.extension.dto

import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.extension.TICKS_PER_MS
import dev.brahmkshatriya.echo.extension.getImageUrl
import dev.brahmkshatriya.echo.extension.toDate
import kotlinx.serialization.Serializable

@Serializable
data class AlbumDto(
    val id: String,
    val name: String,
    val imageTags: ImageTagDto,
    val artistItems: List<ArtistItemDto>,
    val childCount: Int? = null,
    val runTimeTicks: Long? = null,
    val premiereDate: String? = null,
    val overview: String? = null,
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
            artists = this.artistItems.map { Artist(id = it.id, name = it.name) },
            tracks = this.childCount,
            duration = this.runTimeTicks?.div(TICKS_PER_MS),
            releaseDate = premiereDate?.toDate(),
            description = overview?.takeIf(String::isNotEmpty),
        )
    }
}
