package dev.brahmkshatriya.echo.extension.dto

import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.extension.TICKS_PER_MS
import dev.brahmkshatriya.echo.extension.getImageUrl
import dev.brahmkshatriya.echo.extension.toDate
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistDto(
    val id: String,
    val name: String,
    val imageTags: ImageTagDto,
    val childCount: Int? = null,
    val overview: String? = null,
    val runTimeTicks: Long? = null,
    val dateCreated: String? = null,
) : MediaItem {
    override fun toMediaItem(serverUrl: String): EchoMediaItem {
        return EchoMediaItem.Lists.PlaylistItem(
            toPlaylist(serverUrl),
        )
    }

    fun toPlaylist(serverUrl: String): Playlist {
        return Playlist(
            id = this.id,
            title = this.name,
            // There doesn't seem to be any nice way of retrieving this, but who's gonna use this
            // extension on a server where they can't edit the playlists anyways
            isEditable = true,
            cover = this.imageTags.primary?.getImageUrl(serverUrl, this.id),
            tracks = this.childCount,
            duration = this.runTimeTicks?.div(TICKS_PER_MS),
            creationDate = this.dateCreated?.toDate(),
            description = this.overview,
        )
    }
}
