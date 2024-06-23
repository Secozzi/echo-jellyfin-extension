package dev.brahmkshatriya.echo.extension.models

import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extension.TicksPerMs
import dev.brahmkshatriya.echo.extension.toImage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class TrackDto(
    @SerialName("Album") val albumName: String,
    @SerialName("AlbumId") val albumId: String,
    @SerialName("AlbumPrimaryImageTag") val albumPImgTag: String? = null,
    @SerialName("ArtistItems") val artists: List<ArtistItemsDto>,
    @SerialName("Id") val id: String,
    @SerialName("ImageTags") val imageTags: ImageTagDto,
    @SerialName("MediaSources") val mediaSources: List<MediaSource>? = null,
    @SerialName("Name") val name: String,
    @SerialName("PlaylistItemId") val playlistItemId: String? = null,
    @SerialName("RunTimeTicks") val runTime: Long? = null,
    @SerialName("UserData") val userData: UserData,
) : MediaItem {
    override fun toMediaItem(serverUrl: String): EchoMediaItem {
        return EchoMediaItem.TrackItem(
            toTrack(serverUrl),
        )
    }

    fun toTrack(serverUrl: String): Track {
        return Track(
            album = Album(id = this.albumId, title = this.albumName),
            artists = this.artists.map { artist ->
                Artist(id = artist.id, name = artist.name)
            },
            audioStreamables = this.mediaSources?.firstOrNull()?.let {
                listOf(Streamable(this.id, it.bitrate))
            } ?: emptyList(),
            cover = this.imageTags.primary.toImage(serverUrl, this.id)
                ?: this.albumPImgTag.toImage(serverUrl, this.albumId),
            duration = this.runTime?.div(TicksPerMs),
            extras = buildMap {
                playlistItemId?.also {
                    put("playlist_item_id", playlistItemId)
                }
            },
            id = this.id,
            liked = userData.isFavorite,
            plays = userData.playCount,
            title = this.name,
            videoStreamable = emptyList(),
        )
    }

    @Serializable
    class UserData(
        @SerialName("IsFavorite") val isFavorite: Boolean,
        @SerialName("PlayCount") val playCount: Int,
    )

    @Serializable
    class MediaSource(
        @SerialName("Bitrate") val bitrate: Int,
    )
}
