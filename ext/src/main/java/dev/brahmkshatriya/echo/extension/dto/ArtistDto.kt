package dev.brahmkshatriya.echo.extension.dto

import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.extension.getImageUrl
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl

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
        return EchoMediaItem.Profile.ArtistItem(
            toArtist(serverUrl),
        )
    }

    fun toArtist(serverUrl: String): Artist {
        return Artist(
            id = this.id,
            name = this.name,
            cover = this.imageTags.primary.getImageUrl(serverUrl, this.id),
            description = this.overview,
            banners = this.imageTags.banner.getImageUrl(serverUrl, this.id, "Banner")?.let(::listOf)
                ?: backdropImageTags?.mapIndexed { idx, tag ->
                    tag.getBackgroundUrl(serverUrl, this.id, idx)
                }.orEmpty(),
            isFollowing = userData?.isFavorite == true,
        )
    }
}

private fun String.getBackgroundUrl(serverUrl: String, id: String, index: Int): ImageHolder {
    return serverUrl.toHttpUrl().newBuilder().apply {
        addPathSegment("Items")
        addPathSegment(id)
        addPathSegment("Images")
        addPathSegment("Backdrop")
        addPathSegment(index.toString())
        addQueryParameter("tag", this@getBackgroundUrl)
    }.build().toString().toImageHolder()
}
