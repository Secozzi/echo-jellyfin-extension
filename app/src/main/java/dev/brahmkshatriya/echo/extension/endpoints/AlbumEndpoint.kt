package dev.brahmkshatriya.echo.extension.endpoints

import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.extension.makeApiRequest
import dev.brahmkshatriya.echo.extension.models.AlbumDto
import dev.brahmkshatriya.echo.extension.parseAs
import okhttp3.OkHttpClient

class AlbumEndpoint(client: OkHttpClient) : EndPoint<AlbumDto>(
    client,
    "MusicAlbum",
    AlbumDto.serializer(),
) {
    fun loadAlbum(album: Album): Album {
        val url = userCredentials.urlBuilder().apply {
            addPathSegment("Users")
            addPathSegment(userCredentials.userId)
            addPathSegment("Items")
            addPathSegment(album.id)
        }.build()

        return client.newCall(
            makeApiRequest(userCredentials, url),
        ).execute().parseAs<AlbumDto>().toAlbum(userCredentials.serverUrl)
    }
}
