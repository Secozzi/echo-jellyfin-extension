package dev.brahmkshatriya.echo.extension.endpoints

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extension.DELETE
import dev.brahmkshatriya.echo.extension.POST
import dev.brahmkshatriya.echo.extension.TicksPerMs
import dev.brahmkshatriya.echo.extension.getHeaders
import dev.brahmkshatriya.echo.extension.makeApiRequest
import dev.brahmkshatriya.echo.extension.models.ItemsListDto
import dev.brahmkshatriya.echo.extension.models.PlaylistDto
import dev.brahmkshatriya.echo.extension.models.TrackDto
import dev.brahmkshatriya.echo.extension.parseAs
import dev.brahmkshatriya.echo.extension.toImage
import dev.brahmkshatriya.echo.extension.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import java.io.File

class PlaylistEndpoint(client: OkHttpClient) : EndPoint<PlaylistDto>(
    client,
    "Playlist",
    PlaylistDto.serializer(),
) {
    fun createPlaylist(title: String, description: String?): Playlist {
        val body = buildJsonObject {
            put("MediaType", "Audio")
            put("Name", title)
            put("Overview", description ?: "")
            put("UserId", userCredentials.userId)
        }.toRequestBody()

        val url = userCredentials.serverUrl.toHttpUrl().newBuilder().apply {
            addPathSegment("playlists")
        }.build()

        val data = client.newCall(
            makeApiRequest(userCredentials, url, body),
        ).execute().parseAs<IdDto>()

        return Playlist(
            id = data.id,
            title = title,
            description = description,
            isEditable = true,
        )
    }

    @Serializable
    class IdDto(
        @SerialName("Id") val id: String,
    )

    fun deletePlaylist(playlist: Playlist) {
        val url = userCredentials.serverUrl.toHttpUrl().newBuilder().apply {
            addPathSegment("Items")
            addPathSegment(playlist.id)
        }.build()

        val headers = getHeaders(userCredentials)
        client.newCall(
            DELETE(url, headers = headers),
        ).execute()
    }

    fun addToPlaylist(playlist: Playlist, new: List<Track>) {
        val url = userCredentials.serverUrl.toHttpUrl().newBuilder().apply {
            addPathSegment("Playlists")
            addPathSegment(playlist.id)
            addPathSegment("Items")
            addQueryParameter("Ids", new.joinToString(",") { it.id })
            addQueryParameter("UserId", userCredentials.userId)
        }.build()

        val headers = getHeaders(userCredentials)
        client.newCall(
            POST(url, headers = headers),
        ).execute()
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun editPlaylistMetadata(playlist: Playlist, title: String, description: String?) {
        val url = userCredentials.serverUrl.toHttpUrl().newBuilder().apply {
            addPathSegment("Items")
            addPathSegment(playlist.id)
        }.build()

        val body = buildJsonObject {
            putJsonArray("AirDays") {}
            put("AirsAfterSeasonNumber", "")
            put("AirsBeforeEpisodeNumber", "")
            put("AirsBeforeSeasonNumber", "")
            put("AirTime", "")
            put("Album", "")
            putJsonArray("AlbumArtists") {}
            putJsonArray("ArtistItems") {}
            put("AspectRatio", "")
            put("CommunityRating", "")
            put("CriticRating", "")
            put("CustomRating", "")
            put("DateCreated", "2024-06-20T20:11:00.360Z")
            put("DisplayOrder", "")
            put("EndDate", null)
            put("ForcedSortName", "")
            putJsonArray("Genres") {}
            put("Height", "")
            put("Id", "4f2973a10fe0ce571445ab41b261a756")
            put("IndexNumber", null)
            put("LockData", false)
            putJsonArray("LockedFields") {}
            put("Name", title)
            put("OfficialRating", "")
            put("OriginalTitle", "")
            put("Overview", description ?: "")
            put("ParentIndexNumber", null)
            putJsonArray("People") {}
            put("PreferredMetadataCountryCode", "")
            put("PreferredMetadataLanguage", "")
            put("PremiereDate", null)
            put("ProductionYear", "")
            putJsonObject("ProviderIds") {}
            put("Status", "")
            putJsonArray("Studios") {}
            putJsonArray("Taglines") {}
            putJsonArray("Tags") {}
            put("Video3DFormat", "")
        }.toRequestBody()

        client.newCall(
            makeApiRequest(userCredentials, url, body),
        ).execute()
    }

    fun editPlaylistCover(playlist: Playlist, cover: File?) {
        val url = userCredentials.urlBuilder().apply {
            addPathSegment("Items")
            addPathSegment(playlist.id)
            addPathSegment("Images")
            addPathSegment("Primary")
        }.build()

        client.newCall(
            makeApiRequest(userCredentials, url, cover.toRequestBody()),
        )
    }

    fun moveTrackInPlaylist(playlist: Playlist, tracks: List<Track>, fromIndex: Int, toIndex: Int) {
        val url = userCredentials.urlBuilder().apply {
            addPathSegment("Playlists")
            addPathSegment(playlist.id)
            addPathSegment("Items")
            addPathSegment(tracks[fromIndex].extras["playlist_item_id"]!!)
            addPathSegment("Move")
            addPathSegment(toIndex.toString())
        }.build()

        val headers = getHeaders(userCredentials)
        client.newCall(
            POST(url, headers = headers),
        ).execute()
    }

    fun removeTracksFromPlaylist(playlist: Playlist, tracks: List<Track>, indexes: List<Int>) {
        val url = userCredentials.serverUrl.toHttpUrl().newBuilder().apply {
            addPathSegment("Playlists")
            addPathSegment(playlist.id)
            addPathSegment("Items")
            addQueryParameter("EntryIds", indexes.joinToString(",") { tracks[it].extras["playlist_item_id"]!! })
        }.build()

        val headers = getHeaders(userCredentials)
        client.newCall(
            DELETE(url, headers = headers),
        ).execute()
    }

    fun loadPlaylist(playlist: Playlist): Playlist {
        val serverUrl = userCredentials.serverUrl.toHttpUrl()

        val isPlaylistEditableUrl = serverUrl.newBuilder().apply {
            addPathSegment("Playlists")
            addPathSegment(playlist.id)
            addPathSegment("Users")
            addPathSegment(userCredentials.userId)
        }.build()
        val isPlaylistEditable = client.newCall(
            makeApiRequest(userCredentials, isPlaylistEditableUrl),
        ).execute().parseAs<CanEditDto>().canEdit

        val playlistUrl = serverUrl.newBuilder().apply {
            addPathSegment("Users")
            addPathSegment(userCredentials.userId)
            addPathSegment("Items")
            addPathSegment(playlist.id)
        }.build()
        val playlistData = client.newCall(
            makeApiRequest(userCredentials, playlistUrl),
        ).execute().parseAs<PlaylistDto>()

        return Playlist(
            cover = playlistData.imageTags.primary.toImage(userCredentials.serverUrl, playlistData.id),
            description = playlistData.overview,
            duration = playlistData.runTime?.div(TicksPerMs),
            id = playlistData.id,
            isEditable = isPlaylistEditable,
            title = playlistData.name,
            tracks = playlistData.childCount,
        )
    }

    @Serializable
    class CanEditDto(
        @SerialName("CanEdit") val canEdit: Boolean,
    )

    fun loadTracks(playlist: Playlist): PagedData<Track> = PagedData.Single {
        val url = userCredentials.urlBuilder().apply {
            addPathSegment("Playlists")
            addPathSegment(playlist.id)
            addPathSegment("Items")
            addQueryParameter("UserId", userCredentials.userId)
            addQueryParameter("Fields", "ParentId")
        }.build()

        val items = withContext(Dispatchers.IO) {
            client.newCall(
                makeApiRequest(userCredentials, url),
            ).execute().parseAs<ItemsListDto<TrackDto>>()
        }

        items.items.map { it.toTrack(userCredentials.serverUrl) }
    }

    fun likeTrack(track: Track, liked: Boolean): Boolean {
        val url = userCredentials.urlBuilder().apply {
            addPathSegment("Users")
            addPathSegment(userCredentials.userId)
            addPathSegment("FavoriteItems")
            addPathSegment(track.id)
        }.build()

        val headers = getHeaders(userCredentials)
        val request = if (liked) {
            POST(url, headers = headers)
        } else {
            DELETE(url, headers = headers)
        }

        return client.newCall(request).execute()
            .parseAs<IsFavoriteDto>().isFavorite
    }

    @Serializable
    class IsFavoriteDto(
        @SerialName("IsFavorite") val isFavorite: Boolean,
    )
}
