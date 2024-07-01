package dev.brahmkshatriya.echo.extension.models

import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.extension.TicksPerMs
import dev.brahmkshatriya.echo.extension.toImage
import kotlinx.serialization.Serializable

@Serializable
class PlaylistDto(
    val name: String,
    val id: String,
    val overview: String? = null,
    val runTimeTicks: Long? = null,
    val childCount: Int? = null,
    val imageTags: ImageTagDto,
) : MediaItem {
    override fun toMediaItem(serverUrl: String): EchoMediaItem {
        return EchoMediaItem.Lists.PlaylistItem(
            toPlaylist(serverUrl),
        )
    }

    fun toPlaylist(serverUrl: String): Playlist {
        return Playlist(
            cover = this.imageTags.primary.toImage(serverUrl, this.id),
            description = this.overview,
            duration = this.runTimeTicks?.div(TicksPerMs),
            id = this.id,

            // There doesn't seem to be any nice way of retrieving this, but who's gonna use this
            // extension on a server where they can't edit the playlists anyways
            isEditable = true,
            title = this.name,
            tracks = this.childCount,
        )
    }
}
