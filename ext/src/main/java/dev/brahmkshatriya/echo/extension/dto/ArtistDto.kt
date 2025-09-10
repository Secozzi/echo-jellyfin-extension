package dev.brahmkshatriya.echo.extension.dto

import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.extension.getImageUrl
import kotlinx.serialization.Serializable

@Serializable
data class ArtistDto(
    val id: String,
    val name: String,
    val imageTags: ImageTagDto,
    val overview: String? = null,
    val backdropImageTags: List<String>? = null,
    val userData: UserData? = null,
) : MediaItem {
    override fun toMediaItem(serverUrl: String): EchoMediaItem {
        return toArtist(serverUrl)
    }

    fun toArtist(serverUrl: String): Artist {
        return Artist(
            id = this.id,
            name = this.name,
            cover = this.imageTags.primary?.getImageUrl(serverUrl, this.id),
            bio = this.overview,
            banners = this.imageTags.banner?.getImageUrl(serverUrl, this.id, "Banner")?.let(::listOf)
                ?: backdropImageTags?.mapIndexed { idx, tag ->
                    tag.getImageUrl(serverUrl, this.id, "Backdrop", idx)
                }.orEmpty(),
            isFollowable = false,
            isLikeable = true,
        )
    }
}
