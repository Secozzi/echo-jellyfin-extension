package dev.brahmkshatriya.echo.extension.models

import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extension.TicksPerMs
import dev.brahmkshatriya.echo.extension.toImage
import kotlinx.serialization.Serializable

@Serializable
class TrackDto(
    val album: String,
    val albumId: String,
    val albumPrimaryImageTag: String? = null,
    val artistItems: List<ArtistItemsDto>,
    val id: String,
    val imageTags: ImageTagDto,
    val mediaSources: List<MediaSource>? = null,
    val name: String,
    val playlistItemId: String? = null,
    val runTimeTicks: Long? = null,
    val userData: UserData,
) : MediaItem {
    override fun toMediaItem(serverUrl: String): EchoMediaItem {
        return EchoMediaItem.TrackItem(
            toTrack(serverUrl),
        )
    }

    fun toTrack(serverUrl: String): Track {
        return Track(
            album = Album(id = this.albumId, title = this.album),
            artists = this.artistItems.map { artist ->
                Artist(id = artist.id, name = artist.name)
            },
            audioStreamables = this.mediaSources?.firstOrNull()?.let {
                listOf(Streamable(this.id, it.bitrate))
            } ?: emptyList(),
            cover = this.imageTags.primary.toImage(serverUrl, this.id)
                ?: this.albumPrimaryImageTag.toImage(serverUrl, this.albumId),
            duration = this.runTimeTicks?.div(TicksPerMs),
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
        val isFavorite: Boolean,
        val playCount: Int,
    )

    @Serializable
    class MediaSource(
        val bitrate: Int,
    )
}
