package dev.brahmkshatriya.echo.extension.endpoints

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Request
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.StreamableAudio
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extension.makeApiRequest
import dev.brahmkshatriya.echo.extension.models.ItemsListDto
import dev.brahmkshatriya.echo.extension.models.TrackDto
import dev.brahmkshatriya.echo.extension.parseAs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

class TrackEndpoint(client: OkHttpClient) : EndPoint<TrackDto>(
    client,
    "Audio",
    TrackDto.serializer(),
) {
    fun loadTrack(track: Track): Track {
        val url = userCredentials.urlBuilder().apply {
            addPathSegment("Users")
            addPathSegment(userCredentials.userId)
            addPathSegment("Items")
            addPathSegment(track.id)
        }.build()

        return client.newCall(
            makeApiRequest(userCredentials, url),
        ).execute().parseAs<TrackDto>().toTrack(userCredentials.serverUrl)
    }

    fun getStreamable(streamable: Streamable): StreamableAudio {
        val url = userCredentials.urlBuilder().apply {
            addPathSegment("Audio")
            addPathSegment(streamable.id)
            addPathSegment("Universal")
            addQueryParameter("UserId", userCredentials.userId)
            addQueryParameter("api_key", userCredentials.accessToken)
        }.build().toString()

        return StreamableAudio.StreamableRequest(Request(url))
    }

    fun getRadio(itemId: String): PagedData<Track> {
        return PagedData.Single {
            val url = userCredentials.urlBuilder().apply {
                addPathSegment("Items")
                addPathSegment(itemId)
                addPathSegment("InstantMix")
                addQueryParameter("UserId", userCredentials.userId)
                addQueryParameter("Limit", "200")
            }.build()

            val items = withContext(Dispatchers.IO) {
                client.newCall(
                    makeApiRequest(userCredentials, url),
                ).execute().parseAs<ItemsListDto<TrackDto>>()
            }

            items.items.map { it.toTrack(userCredentials.serverUrl) }
        }
    }
}
