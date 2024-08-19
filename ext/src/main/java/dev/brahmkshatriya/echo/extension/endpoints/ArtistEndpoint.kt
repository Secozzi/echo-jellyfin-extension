package dev.brahmkshatriya.echo.extension.endpoints

import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.extension.makeApiRequest
import dev.brahmkshatriya.echo.extension.models.ArtistDto
import dev.brahmkshatriya.echo.extension.parseAs
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

class ArtistEndpoint(client: OkHttpClient) : EndPoint<ArtistDto>(
    client,
    "Artist",
    ArtistDto.serializer(),
) {
    override fun createUrl(
        itemType: String,
        sortBy: String,
        fields: String?,
        limit: String,
        sortOrder: String,
        startIndex: String,
        builderBlock: HttpUrl.Builder.() -> Unit,
    ): HttpUrl {
        return super.createUrl(
            itemType,
            sortBy,
            fields,
            limit,
            sortOrder,
            startIndex,
        ) {
            encodedPath("/")
            addPathSegment("artists")
            addPathSegment("albumArtists")
            apply(builderBlock)
            setQueryParameter("UserId", userCredentials.userId)
        }
    }

    fun loadArtist(artist: Artist): Artist {
        val url = userCredentials.urlBuilder().apply {
            addPathSegment("Users")
            addPathSegment(userCredentials.userId)
            addPathSegment("Items")
            addPathSegment(artist.id)
        }.build()

        return client.newCall(
            makeApiRequest(userCredentials, url),
        ).execute().parseAs<ArtistDto>().toArtist(userCredentials.serverUrl)
    }
}
