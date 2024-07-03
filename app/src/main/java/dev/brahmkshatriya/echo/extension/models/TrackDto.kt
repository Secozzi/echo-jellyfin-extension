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
    val id: String,
    val name: String,
    private val album: String,
    private val albumId: String,
    private val albumPrimaryImageTag: String? = null,
    private val artistItems: List<ArtistItemsDto>,
    private val imageTags: ImageTagDto,
    private val mediaSources: List<MediaSource>? = null,
    private val playlistItemId: String? = null,
    private val runTimeTicks: Long? = null,
    private val userData: UserData,
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
