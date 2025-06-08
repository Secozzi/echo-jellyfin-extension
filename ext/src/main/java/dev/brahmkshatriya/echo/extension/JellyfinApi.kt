package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.helpers.ClientException
import dev.brahmkshatriya.echo.common.helpers.Page
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Request.Companion.toRequest
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Streamable.Media.Companion.toMedia
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.extension.dto.AlbumDto
import dev.brahmkshatriya.echo.extension.dto.ArtistDto
import dev.brahmkshatriya.echo.extension.dto.IdDto
import dev.brahmkshatriya.echo.extension.dto.ItemListDto
import dev.brahmkshatriya.echo.extension.dto.LoginDto
import dev.brahmkshatriya.echo.extension.dto.MediaItem
import dev.brahmkshatriya.echo.extension.dto.PlaylistDto
import dev.brahmkshatriya.echo.extension.dto.TrackDto
import dev.brahmkshatriya.echo.extension.dto.toShelf
import extension.ext.BuildConfig
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.put
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import kotlin.collections.component1
import kotlin.collections.component2

class JellyfinApi {
    private val json = Json {
        ignoreUnknownKeys = true
        namingStrategy = PascalCaseToCamelCase
    }

    private var userCredentials = UserCredentials.EMPTY

    private val client = OkHttpClient().newBuilder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Accept", "application/json, application/octet-stream;q=0.9, */*;q=0.8")
                .build()

            if (request.url.encodedPath.endsWith("AuthenticateByName")) {
                return@addInterceptor chain.proceed(request)
            }

            if (userCredentials.accessToken.isEmpty()) {
                return@addInterceptor Response.Builder().apply {
                    request(request)
                    protocol(Protocol.HTTP_1_1)
                    code(401)
                    message("Unauthorized")
                }.build()
            }

            val authRequest = request.newBuilder()
                .addHeader("X-MediaBrowser-Token", userCredentials.accessToken)
                .build()

            chain.proceed(authRequest)
        }
        .build()

    // ================ Login =================

    suspend fun onLogin(data: Map<String, String?>, deviceId: String): List<User> {
        val serverUrl = data["address"]!!

        val body = buildJsonObject {
            put("Username", data["username"]!!)
            put("Pw", data["password"]!!)
        }.toRequestBody()

        val headers = Headers.headersOf(
            "X-Emby-Authorization",
            """MediaBrowser Client="${BuildConfig.APP_NAME}", Device="${BuildConfig.DEVICE_NAME}", DeviceId="$deviceId", Version="${BuildConfig.APP_VER}"""",
        )

        val url = serverUrl.toHttpUrl().newBuilder().apply {
            addPathSegment("Users")
            addPathSegment("AuthenticateByName")
        }.build()

        val loginData = client.post(url, headers, body).parseAs<LoginDto>()

        val user = User(
            id = loginData.user.id,
            name = loginData.user.name,
            cover = "$serverUrl/Users/${loginData.user.id}/Images/Primary".toImageHolder(),
            extras = mapOf(
                "accessToken" to loginData.accessToken,
                "serverUrl" to serverUrl,
            ),
        )

        return listOf(user)
    }

    fun setUser(user: User?) {
        userCredentials = user?.let {
            UserCredentials(
                userId = it.id,
                userName = it.name,
                accessToken = it.extras["accessToken"]!!,
                serverUrl = it.extras["serverUrl"]!!,
            )
        } ?: UserCredentials.EMPTY
    }

    fun getUser(): User? {
        if (userCredentials.accessToken.isEmpty()) return null

        return User(
            id = userCredentials.userId,
            name = userCredentials.userName,
            cover = "${userCredentials.serverUrl}/Users/${userCredentials.userId}/Images/Primary".toImageHolder(),
        )
    }

    // ================ Albums ================

    private fun buildAlbumUrl(
        sortBy: String,
        sortOrder: String,
        startIndex: Int,
        limit: Int,
        extraParams: Map<String, String> = emptyMap(),
    ): HttpUrl {
        checkAuth()

        return getUrlBuilder().apply {
            addPathSegment("Users")
            addPathSegment(userCredentials.userId)
            addPathSegment("Items")
            addQueryParameter("IncludeItemTypes", "MusicAlbum")
            addQueryParameter("Recursive", "true")
            addQueryParameter("Limit", limit.toString())
            addQueryParameter("StartIndex", startIndex.toString())
            addQueryParameter("SortBy", sortBy)
            addQueryParameter("SortOrder", sortOrder)
            extraParams.forEach { (key, value) ->
                if (value.isNotBlank()) {
                    addQueryParameter(key, value)
                }
            }
        }.build()
    }

    suspend fun getAlbumShelf(
        query: String = "",
        shelfTitle: String,
        sortBy: String,
        sortOrder: String = "Descending",
        startIndex: Int = 0,
        limit: Int = 15,
    ): Shelf {
        val url = buildAlbumUrl(
            sortBy = sortBy,
            sortOrder = sortOrder,
            startIndex = startIndex,
            limit = limit,
            extraParams = mapOf(
                "SearchTerm" to query,
            ),
        )

        return getShelf<AlbumDto>(
            url = url,
            shelfTitle = shelfTitle,
            limit = limit,
            serializer = AlbumDto.serializer(),
        )
    }

    fun getAlbumPage(
        query: String = "",
        sortBy: String = "DateCreated,SortName",
        sortOrder: String = "Descending",
        startIndex: Int = 0,
        limit: Int = 50,
    ): PagedData<Shelf> {
        val url = buildAlbumUrl(
            sortBy = sortBy,
            sortOrder = sortOrder,
            startIndex = startIndex,
            limit = limit,
            extraParams = mapOf(
                "SearchTerm" to query,
            ),
        )

        return getContinuousData<Shelf, AlbumDto>(url, limit, AlbumDto.serializer()) {
            it.toShelf(userCredentials.serverUrl)
        }
    }

    suspend fun getAlbum(album: Album): Album {
        checkAuth()

        val url = getUrlBuilder().apply {
            addPathSegment("Users")
            addPathSegment(userCredentials.userId)
            addPathSegment("Items")
            addPathSegment(album.id)
        }.build()

        return client.get(url).parseAs<AlbumDto>().toAlbum(userCredentials.serverUrl)
    }

    fun getAlbumTracks(album: Album, limit: Int = 200): PagedData<Track> {
        checkAuth()

        val url = getUrlBuilder().apply {
            addPathSegment("Users")
            addPathSegment(userCredentials.userId)
            addPathSegment("Items")
            addQueryParameter("IncludeItemTypes", "Audio")
            addQueryParameter("ParentId", album.id)
            addQueryParameter("SortBy", "ParentIndexNumber,IndexNumber,SortName")
            addQueryParameter("Limit", limit.toString())
        }.build()

        return getContinuousData<Track, TrackDto>(url, limit, TrackDto.serializer()) {
            it.toTrack(userCredentials.serverUrl)
        }
    }

    // =============== Artists ================

    private fun buildArtistUrl(
        sortBy: String,
        sortOrder: String,
        startIndex: Int,
        limit: Int,
        extraParams: Map<String, String> = emptyMap(),
    ): HttpUrl {
        checkAuth()

        return getUrlBuilder().apply {
            addPathSegment("Artists")
            addPathSegment("AlbumArtists")
            addQueryParameter("ImageTypeLimit", "1")
            addQueryParameter("Recursive", "true")
            addQueryParameter("Limit", limit.toString())
            addQueryParameter("StartIndex", startIndex.toString())
            addQueryParameter("SortBy", sortBy)
            addQueryParameter("SortOrder", sortOrder)
            addQueryParameter("UserId", userCredentials.userId)
            extraParams.forEach { (key, value) ->
                if (value.isNotBlank()) {
                    addQueryParameter(key, value)
                }
            }
        }.build()
    }

    suspend fun getArtistShelf(
        query: String = "",
        shelfTitle: String,
        sortBy: String,
        sortOrder: String = "Descending",
        startIndex: Int = 0,
        limit: Int = 15,
    ): Shelf {
        val url = buildArtistUrl(
            sortBy = sortBy,
            sortOrder = sortOrder,
            startIndex = startIndex,
            limit = limit,
            extraParams = mapOf(
                "SearchTerm" to query,
            ),
        )

        return getShelf<ArtistDto>(
            url = url,
            shelfTitle = shelfTitle,
            limit = limit,
            serializer = ArtistDto.serializer(),
        )
    }

    fun getArtistPage(
        query: String = "",
        sortBy: String = "SortName,Name",
        sortOrder: String = "Ascending",
        startIndex: Int = 0,
        limit: Int = 50,
    ): PagedData<Shelf> {
        val url = buildArtistUrl(
            sortBy = sortBy,
            sortOrder = sortOrder,
            startIndex = startIndex,
            limit = limit,
            extraParams = mapOf(
                "SearchTerm" to query,
            ),
        )

        return getContinuousData<Shelf, ArtistDto>(url, limit, ArtistDto.serializer()) {
            it.toShelf(userCredentials.serverUrl)
        }
    }

    suspend fun getArtist(artist: Artist): Artist {
        checkAuth()

        val url = getUrlBuilder().apply {
            addPathSegment("Users")
            addPathSegment(userCredentials.userId)
            addPathSegment("Items")
            addPathSegment(artist.id)
        }.build()

        return client.get(url).parseAs<ArtistDto>().toArtist(userCredentials.serverUrl)
    }

    suspend fun getArtistAlbums(
        artist: Artist,
        shelfTitle: String,
        sortBy: String,
        sortOrder: String = "Descending",
        startIndex: Int = 0,
        limit: Int = 200,
    ): Shelf {
        val url = buildAlbumUrl(
            sortBy = sortBy,
            sortOrder = sortOrder,
            startIndex = startIndex,
            limit = limit,
            extraParams = mapOf(
                "AlbumArtistIds" to artist.id,
            ),
        )

        return getShelf<AlbumDto>(
            url = url,
            shelfTitle = shelfTitle,
            limit = limit,
            serializer = AlbumDto.serializer(),
        )
    }

    suspend fun getSimilarArtists(
        artist: Artist,
        shelfTitle: String,
        limit: Int = 10,
    ): Shelf {
        val url = getUrlBuilder().apply {
            addPathSegment("Artists")
            addPathSegment(artist.id)
            addPathSegment("Similar")
            addQueryParameter("Limit", limit.toString())
        }.build()

        val data = client.get(url).parseAs<ItemListDto<ArtistDto>>()
        val items = data.items.map { it.toMediaItem(userCredentials.serverUrl) }

        return Shelf.Lists.Items(
            title = shelfTitle,
            list = items,
            more = null,
        )
    }

    // ============ Follow Artist =============

    suspend fun followArtist(artist: Artist, follow: Boolean) {
        checkAuth()

        favoriteItem(artist.id, follow)
    }

    // ============== Playlists ===============

    private fun buildPlaylistUrl(
        sortBy: String,
        sortOrder: String,
        startIndex: Int,
        limit: Int,
        extraParams: Map<String, String> = emptyMap(),
    ): HttpUrl {
        checkAuth()

        return getUrlBuilder().apply {
            addPathSegment("Users")
            addPathSegment(userCredentials.userId)
            addPathSegment("Items")
            addQueryParameter("IncludeItemTypes", "Playlist")
            addQueryParameter("Recursive", "true")
            addQueryParameter("Limit", limit.toString())
            addQueryParameter("StartIndex", startIndex.toString())
            addQueryParameter("SortBy", sortBy)
            addQueryParameter("SortOrder", sortOrder)
            extraParams.forEach { (key, value) ->
                if (value.isNotBlank()) {
                    addQueryParameter(key, value)
                }
            }
        }.build()
    }

    suspend fun getPlaylistShelf(
        query: String = "",
        shelfTitle: String,
        sortBy: String,
        sortOrder: String = "Descending",
        startIndex: Int = 0,
        limit: Int = 15,
    ): Shelf {
        val url = buildPlaylistUrl(
            sortBy = sortBy,
            sortOrder = sortOrder,
            startIndex = startIndex,
            limit = limit,
            extraParams = mapOf(
                "SearchTerm" to query,
            ),
        )

        return getShelf<PlaylistDto>(
            url = url,
            shelfTitle = shelfTitle,
            limit = limit,
            serializer = PlaylistDto.serializer(),
        )
    }

    fun getPlaylistPage(
        query: String = "",
        sortBy: String = "SortName",
        sortOrder: String = "Ascending",
        startIndex: Int = 0,
        limit: Int = 50,
    ): PagedData<Shelf> {
        val url = buildPlaylistUrl(
            sortBy = sortBy,
            sortOrder = sortOrder,
            startIndex = startIndex,
            limit = limit,
            extraParams = mapOf(
                "SearchTerm" to query,
            ),
        )

        return getContinuousData<Shelf, PlaylistDto>(url, limit, PlaylistDto.serializer()) {
            it.toShelf(userCredentials.serverUrl)
        }
    }

    suspend fun getPlaylist(playlist: Playlist): Playlist {
        checkAuth()

        val url = getUrlBuilder().apply {
            addPathSegment("Users")
            addPathSegment(userCredentials.userId)
            addPathSegment("Items")
            addPathSegment(playlist.id)
        }.build()

        return client.get(url).parseAs<PlaylistDto>().toPlaylist(userCredentials.serverUrl)
    }

    fun getPlaylistTracks(playlist: Playlist, limit: Int = 200): PagedData<Track> {
        checkAuth()

        val url = getUrlBuilder().apply {
            addPathSegment("Playlists")
            addPathSegment(playlist.id)
            addPathSegment("Items")
            addQueryParameter("IncludeItemTypes", "Audio")
            addQueryParameter("Limit", limit.toString())
            addQueryParameter("UserId", userCredentials.userId)
        }.build()

        return getContinuousData<Track, TrackDto>(url, limit, TrackDto.serializer()) {
            it.toTrack(userCredentials.serverUrl)
        }
    }

    // ============ Edit Playlist =============

    suspend fun getPlaylists(): List<Playlist> {
        checkAuth()

        val url = getUrlBuilder().apply {
            addPathSegment("Users")
            addPathSegment(userCredentials.userId)
            addPathSegment("Items")
            addQueryParameter("IncludeItemTypes", "Playlist")
            addQueryParameter("Recursive", "true")
            addQueryParameter("SortBy", "SortName")
            addQueryParameter("SortOrder", "Ascending")
        }.build()

        return client.get(url).parseAs<ItemListDto<PlaylistDto>>().items.map {
            it.toPlaylist(userCredentials.serverUrl)
        }
    }

    suspend fun createPlaylist(name: String, description: String?): Playlist {
        checkAuth()

        val body = buildJsonObject {
            put("MediaType", "Audio")
            put("Name", name)
            put("UserId", userCredentials.userId)
        }.toRequestBody()

        val playlistId = client.post(
            url = getUrlBuilder().addPathSegment("Playlists").build(),
            body = body,
        ).parseAs<IdDto>().id

        description?.let {
            editPlaylistMetadata(playlistId, name, it)
        }

        return Playlist(
            id = playlistId,
            title = name,
            isEditable = true,
            description = description,
        )
    }

    suspend fun deletePlaylist(playlist: Playlist) {
        checkAuth()

        val url = getUrlBuilder().apply {
            addPathSegment("Items")
            addPathSegment(playlist.id)
        }.build()

        client.delete(url)
    }

    suspend fun editPlaylistMetadata(id: String, name: String, description: String?) {
        checkAuth()

        val url = getUrlBuilder().apply {
            addPathSegment("Users")
            addPathSegment(userCredentials.userId)
            addPathSegment("Items")
            addPathSegment(id)
        }.build()

        val data = client.get(url)
            .parseAs<Map<String, JsonElement>>()
            .toMutableMap()
            .apply {
                this["Name"] = JsonPrimitive(name)
                description?.let { this["Overview"] = JsonPrimitive(it) }
            }

        val updateUrl = getUrlBuilder().apply {
            addPathSegment("Items")
            addPathSegment(id)
        }.build()

        client.post(updateUrl, body = data.toRequestBody())
    }

    suspend fun addToPlaylist(playlist: Playlist, new: List<Track>) {
        checkAuth()

        val url = getUrlBuilder().apply {
            addPathSegment("Playlists")
            addPathSegment(playlist.id)
            addPathSegment("Items")
            addQueryParameter("Ids", new.joinToString(",") { it.id })
            addQueryParameter("UserId", userCredentials.userId)
        }.build()

        client.post(url)
    }

    suspend fun removeFromPlaylist(
        playlist: Playlist,
        tracks: List<Track>,
        indexes: List<Int>,
    ) {
        checkAuth()

        val url = getUrlBuilder().apply {
            addPathSegment("Playlists")
            addPathSegment(playlist.id)
            addPathSegment("Items")
            addQueryParameter("EntryIds", indexes.joinToString(",") { tracks[it].id })
        }.build()

        client.delete(url)
    }

    suspend fun moveInPlaylist(
        playlist: Playlist,
        tracks: List<Track>,
        fromIndex: Int,
        toIndex: Int,
    ) {
        checkAuth()

        val url = getUrlBuilder().apply {
            addPathSegment("Playlists")
            addPathSegment(playlist.id)
            addPathSegment("Items")
            addPathSegment(tracks[fromIndex].id)
            addPathSegment("Move")
            addPathSegment(toIndex.toString())
        }.build()

        client.post(url)
    }

    // ================ Tracks ================

    private fun buildTrackUrl(
        sortBy: String,
        sortOrder: String,
        startIndex: Int,
        limit: Int,
        extraParams: Map<String, String> = emptyMap(),
    ): HttpUrl {
        checkAuth()

        return getUrlBuilder().apply {
            addPathSegment("Users")
            addPathSegment(userCredentials.userId)
            addPathSegment("Items")
            addQueryParameter("IncludeItemTypes", "Audio")
            addQueryParameter("Recursive", "true")
            addQueryParameter("Limit", limit.toString())
            addQueryParameter("StartIndex", startIndex.toString())
            addQueryParameter("SortBy", sortBy)
            addQueryParameter("SortOrder", sortOrder)
            extraParams.forEach { (key, value) ->
                if (value.isNotBlank()) {
                    addQueryParameter(key, value)
                }
            }
        }.build()
    }

    suspend fun getTrackShelf(
        query: String = "",
        shelfTitle: String,
        sortBy: String,
        sortOrder: String = "Descending",
        startIndex: Int = 0,
        limit: Int = 15,
    ): Shelf {
        val url = buildTrackUrl(
            sortBy = sortBy,
            sortOrder = sortOrder,
            startIndex = startIndex,
            limit = limit,
            extraParams = mapOf(
                "SearchTerm" to query,
            ),
        )

        return getShelf<TrackDto>(
            url = url,
            shelfTitle = shelfTitle,
            limit = limit,
            serializer = TrackDto.serializer(),
        )
    }

    fun getTrackPage(
        query: String = "",
        sortBy: String = "PlayCount,SortName",
        sortOrder: String = "Descending",
        startIndex: Int = 0,
        limit: Int = 50,
    ): PagedData<Shelf> {
        val url = buildTrackUrl(
            sortBy = sortBy,
            sortOrder = sortOrder,
            startIndex = startIndex,
            limit = limit,
            extraParams = mapOf(
                "SearchTerm" to query,
            ),
        )

        return getContinuousData<Shelf, TrackDto>(url, limit, TrackDto.serializer()) {
            it.toShelf(userCredentials.serverUrl)
        }
    }

    suspend fun getTrack(track: Track): Track {
        checkAuth()

        val url = getUrlBuilder().apply {
            addPathSegment("Users")
            addPathSegment(userCredentials.userId)
            addPathSegment("Items")
            addPathSegment(track.id)
        }.build()

        return client.get(url).parseAs<TrackDto>().toTrack(userCredentials.serverUrl)
    }

    fun getStreamableMedia(streamable: Streamable): Streamable.Media {
        val type = streamable.extras["type"]!!

        return if (type == "source") {
            val url = getUrlBuilder().apply {
                addPathSegment("Audio")
                addPathSegment(streamable.id)
                addPathSegment("Universal")
                addQueryParameter("UserId", userCredentials.userId)
                addQueryParameter("ApiKey", userCredentials.accessToken)
            }.build().toString()

            Streamable.Source.Http(
                request = url.toRequest(),
                quality = streamable.quality,
            ).toMedia()
        } else {
            // From https://github.com/jmshrv/finamp/blob/7b902e183a5117e846b2ed7c4a954b3800e21944/lib/services/music_player_background_task.dart#L738
            val url = getUrlBuilder().apply {
                addPathSegment("Audio")
                addPathSegment(streamable.id)
                addPathSegment("main.m3u8")
                addQueryParameter("AudioCodec", "AAC")
                addQueryParameter("AudioSampleRate", "44100")
                addQueryParameter("MaxAudioBitDepth", "16")
                addQueryParameter("AudioBitRate", type)
                addQueryParameter("ApiKey", userCredentials.accessToken)
            }.build().toString()

            Streamable.Source.Http(
                request = url.toRequest(),
                type = Streamable.SourceType.HLS,
                quality = streamable.quality,
            ).toMedia()
        }
    }

    // ============== Like Track ==============

    suspend fun likeTrack(track: Track, isLiked: Boolean) {
        checkAuth()

        favoriteItem(track.id, isLiked)
    }

    // =============== Helpers ================

    suspend fun favoriteItem(itemId: String, isFavorite: Boolean) {
        val url = getUrlBuilder().apply {
            addPathSegment("Users")
            addPathSegment(userCredentials.userId)
            addPathSegment("FavoriteItems")
            addPathSegment(itemId)
        }.build()

        if (isFavorite) {
            client.post(url)
        } else {
            client.delete(url)
        }
    }

    suspend fun <T : MediaItem> getShelf(
        url: HttpUrl,
        shelfTitle: String,
        limit: Int,
        serializer: KSerializer<T>,
    ): Shelf {
        val data = client.get(url).parseAs<ItemListDto<T>>(ItemListDto.serializer(serializer))

        val items = data.items.map { it.toMediaItem(userCredentials.serverUrl) }
        val hasMore = (data.startIndex + limit) < data.totalRecordCount
        val more = getContinuousData<EchoMediaItem, T>(url, limit, serializer) {
            it.toMediaItem(userCredentials.serverUrl)
        }.takeIf { hasMore }

        return Shelf.Lists.Items(
            title = shelfTitle,
            list = items,
            more = more,
        )
    }

    fun <R : Any, T : MediaItem> getContinuousData(
        url: HttpUrl,
        limit: Int,
        serializer: KSerializer<T>,
        transform: (T) -> R,
    ): PagedData.Continuous<R> {
        return PagedData.Continuous { continuation ->
            val newStartIndex = continuation?.toInt() ?: 0
            val newUrl = url.newBuilder()
                .setQueryParameter("StartIndex", newStartIndex.toString())
                .build()

            val newData = client.get(newUrl).parseAs(ItemListDto.serializer(serializer))
            val newContinuation = (newStartIndex + limit)
                .takeIf { it < newData.totalRecordCount }
                ?.toString()

            Page(
                data = newData.items.map { transform(it) },
                continuation = newContinuation,
            )
        }
    }

    // ================ Utils =================

    private fun getUrlBuilder(): HttpUrl.Builder {
        return userCredentials.serverUrl.toHttpUrl().newBuilder()
    }

    fun checkAuth() {
        if (userCredentials.accessToken.isEmpty()) {
            throw ClientException.LoginRequired()
        }
    }

    private inline fun <reified T> Response.parseAs(): T {
        return json.decodeFromStream(body.byteStream())
    }

    private inline fun <reified T> Response.parseAs(serializer: KSerializer<T>): T {
        return json.decodeFromStream(serializer, body.byteStream())
    }

    private inline fun <reified T> T.toRequestBody(): RequestBody {
        return json.encodeToString(this).toRequestBody(
            "application/json".toMediaType(),
        )
    }
}

data class UserCredentials(
    val userId: String,
    val userName: String,
    val accessToken: String,
    val serverUrl: String,
) {
    companion object {
        val EMPTY = UserCredentials("", "", "", "")
    }
}
