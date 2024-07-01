package dev.brahmkshatriya.echo.extension.models

import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.extension.TicksPerMs
import dev.brahmkshatriya.echo.extension.toImage
import kotlinx.serialization.Serializable

@Serializable
class AlbumDto(
    val name: String,
    val id: String,
    val premiereDate: String? = null,
    val overview: String? = null,
    val runTimeTicks: Long? = null,
    val childCount: Int? = null,
    val imageTags: ImageTagDto,
    val artistItems: List<ArtistItemsDto>,
) : MediaItem {
    override fun toMediaItem(serverUrl: String): EchoMediaItem {
        return EchoMediaItem.Lists.AlbumItem(
            toAlbum(serverUrl),
        )
    }

    fun toAlbum(serverUrl: String): Album {
        return Album(
            artists = this.artistItems.map { artist ->
                Artist(id = artist.id, name = artist.name)
            },
            cover = this.imageTags.primary.toImage(serverUrl, this.id),
            description = this.overview,
            duration = this.runTimeTicks?.div(TicksPerMs),
            id = this.id,
            releaseDate = this.premiereDate?.substringBefore("-"),
            title = this.name,
            tracks = this.childCount,
        )
    }
}
