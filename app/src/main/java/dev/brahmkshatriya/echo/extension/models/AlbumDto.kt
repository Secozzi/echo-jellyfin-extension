package dev.brahmkshatriya.echo.extension.models

import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.extension.TicksPerMs
import dev.brahmkshatriya.echo.extension.toImage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class AlbumDto(
    @SerialName("Name") val name: String,
    @SerialName("Id") val id: String,
    @SerialName("PremiereDate") val premiereDate: String? = null,
    @SerialName("Overview") val overview: String? = null,
    @SerialName("RunTimeTicks") val runTime: Long? = null,
    @SerialName("ChildCount") val childCount: Int? = null,
    @SerialName("ImageTags") val imageTags: ImageTagDto,
    @SerialName("ArtistItems") val artists: List<ArtistItemsDto>,
) : MediaItem {
    override fun toMediaItem(serverUrl: String): EchoMediaItem {
        return EchoMediaItem.Lists.AlbumItem(
            toAlbum(serverUrl),
        )
    }

    fun toAlbum(serverUrl: String): Album {
        return Album(
            artists = this.artists.map { artist ->
                Artist(id = artist.id, name = artist.name)
            },
            cover = this.imageTags.primary.toImage(serverUrl, this.id),
            description = this.overview,
            duration = this.runTime?.div(TicksPerMs),
            id = this.id,
            releaseDate = this.premiereDate?.substringBefore("-"),
            title = this.name,
            tracks = this.childCount,
        )
    }
}
