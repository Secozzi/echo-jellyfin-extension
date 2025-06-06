package dev.brahmkshatriya.echo.extension.dto

import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extension.getImageUrl
import kotlinx.serialization.Serializable

@Serializable
data class TrackDto(
    val id: String,
    val name: String,
    private val albumId: String,
    private val albumPrimaryImageTag: String? = null,
    private val imageTags: ImageTagDto,
) : MediaItem {
    override fun toMediaItem(serverUrl: String): EchoMediaItem {
        return EchoMediaItem.TrackItem(
            toTrack(serverUrl),
        )
    }

    fun toTrack(serverUrl: String): Track {
        return Track(
            id = this.id,
            title = this.name,
            cover = this.imageTags.primary?.getImageUrl(serverUrl, this.id)
                ?: this.albumPrimaryImageTag?.getImageUrl(serverUrl, this.albumId),
        )
    }
}
