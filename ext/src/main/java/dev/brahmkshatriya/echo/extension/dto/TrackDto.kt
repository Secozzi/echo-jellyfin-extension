package dev.brahmkshatriya.echo.extension.dto

import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extension.TICKS_PER_MS
import dev.brahmkshatriya.echo.extension.getImageUrl
import dev.brahmkshatriya.echo.extension.toDate
import kotlinx.serialization.Serializable

@Serializable
data class TrackDto(
    val id: String,
    val name: String,
    val albumArtists: List<ArtistItemDto>? = null,
    val album: String? = null,
    val albumId: String? = null,
    val albumPrimaryImageTag: String? = null,
    val imageTags: ImageTagDto,
    val runTimeTicks: Long? = null,
    val userData: UserData,
    val premiereDate: String? = null,
    val overview: String? = null,
    val mediaSources: List<MediaSource>? = null,
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
            artists = this.albumArtists.orEmpty().map { Artist(it.id, it.name) },
            album = album?.let { name -> albumId?.let { id -> Album(id, name) } },
            cover = this.imageTags.primary?.getImageUrl(serverUrl, this.id)
                ?: this.albumPrimaryImageTag?.getImageUrl(serverUrl, this.albumId!!),
            duration = this.runTimeTicks?.div(TICKS_PER_MS),
            plays = this.userData.playCount,
            releaseDate = this.premiereDate?.takeUnless { it.startsWith("0001-01-01") }?.toDate(),
            description = this.overview,
            streamables = this.mediaSources?.firstOrNull()?.let { media ->
                val bitrate = media.mediaStreams.firstOrNull { it.type.equals("audio", true) }?.bitRate
                    ?: media.bitrate

                buildList {
                    add(
                        Streamable.server(
                            id = this@TrackDto.id,
                            quality = bitrate,
                            title = "Source",
                            extras = mapOf("type" to "source"),
                        ),
                    )

                    QUALITIES_LIST.filter { (it * 1000) < bitrate }.reversed().forEach { q ->
                        add(
                            Streamable.server(
                                id = this@TrackDto.id,
                                quality = q * 1000,
                                title = "${q}kbps",
                                extras = mapOf("type" to (q * 1000).toString()),
                            ),
                        )
                    }
                }
            } ?: emptyList(),
            isLiked = this.userData.isFavorite == true,
        )
    }

    @Serializable
    data class MediaSource(
        val bitrate: Int,
        val mediaStreams: List<MediaStream>,
    ) {
        @Serializable
        data class MediaStream(
            val bitRate: Int? = null,
            val type: String? = null,
        )
    }

    companion object {
        private val QUALITIES_LIST = listOf(64, 96, 128, 160, 192, 224, 256, 288, 320)
    }
}
