package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.EditPlaylistCoverClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.LibraryClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.SearchClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.clients.TrackerClient
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.ClientException
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.QuickSearchItem
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.StreamableAudio
import dev.brahmkshatriya.echo.common.models.StreamableVideo
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.SettingList
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.extension.endpoints.AlbumEndpoint
import dev.brahmkshatriya.echo.extension.endpoints.ArtistEndpoint
import dev.brahmkshatriya.echo.extension.endpoints.PlaylistEndpoint
import dev.brahmkshatriya.echo.extension.endpoints.TrackEndpoint
import dev.brahmkshatriya.echo.extension.models.ArtistDto
import dev.brahmkshatriya.echo.extension.models.ItemsListDto
import dev.brahmkshatriya.echo.extension.models.LoginDto
import dev.brahmkshatriya.echo.extension.tabs.getAlbumSearch
import dev.brahmkshatriya.echo.extension.tabs.getArtistSearch
import dev.brahmkshatriya.echo.extension.tabs.getHomeFeed
import dev.brahmkshatriya.echo.extension.tabs.getLibraryAlbums
import dev.brahmkshatriya.echo.extension.tabs.getLibraryAll
import dev.brahmkshatriya.echo.extension.tabs.getLibraryArtists
import dev.brahmkshatriya.echo.extension.tabs.getLibraryHistory
import dev.brahmkshatriya.echo.extension.tabs.getLibraryPlaylists
import dev.brahmkshatriya.echo.extension.tabs.getLibraryTracks
import dev.brahmkshatriya.echo.extension.tabs.getTrackSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

@Suppress("TooManyFunctions")
class JellyfinExtension :
    AlbumClient,
    ArtistClient,
    EditPlaylistCoverClient,
    ExtensionClient,
    HomeFeedClient,
    LibraryClient,
    LoginClient.CustomTextInput,
    RadioClient,
    SearchClient,
    TrackClient,
    TrackerClient {
    private val client = OkHttpClient()

    private var userCredentials = UserCredentials(
        userId = "",
        accessToken = "",
        serverUrl = "",
    )

    // Endpoints
    private val artistEndpoint = ArtistEndpoint(client)
    private val albumEndpoint = AlbumEndpoint(client)
    private val trackEndpoint = TrackEndpoint(client)
    private val playlistEndpoint = PlaylistEndpoint(client)

    @Suppress("EmptyFunctionBlock")
    override suspend fun onExtensionSelected() { }

    // ============== Home Feed ===============

    override fun getHomeFeed(tab: Tab?): PagedData<MediaItemsContainer> {
        require(userCredentials.userId.isNotEmpty()) { throw ClientException.LoginRequired() }
        return getHomeFeed(albumEndpoint, trackEndpoint)
    }

    override suspend fun getHomeTabs(): List<Tab> {
        require(userCredentials.userId.isNotEmpty()) { throw ClientException.LoginRequired() }
        return emptyList()
    }

    // ================ Album =================

    override fun getMediaItems(album: Album): PagedData<MediaItemsContainer> {
        return PagedData.Single {
            withContext(Dispatchers.IO) {
                listOf(
                    albumEndpoint.getItemsContainer(
                        containerName = "More from this artist",
                        sortBy = "SortName",
                        sortOrder = "Descending",
                        limit = 15,
                    ) {
                        addQueryParameter(
                            "AlbumArtistIds",
                            album.artists.joinToString(",") { it.id },
                        )
                        addQueryParameter(
                            "ExcludeItemIds",
                            album.id,
                        )
                    },
                )
            }
        }
    }

    override suspend fun loadAlbum(small: Album): Album {
        return albumEndpoint.loadAlbum(small)
    }

    override fun loadTracks(album: Album): PagedData<Track> {
        return PagedData.Single {
            val tracksItems = withContext(Dispatchers.IO) {
                trackEndpoint.getItemsList(
                    sortBy = "ParentIndexNumber,IndexNumber,SortName",
                    fields = "ParentId",
                    limit = 15,
                ) {
                    addQueryParameter("ParentId", album.id)
                    removeAllQueryParameters("Limit")
                }
            }

            tracksItems.items.map {
                it.toTrack(userCredentials.serverUrl)
            }
        }
    }

    // ================ Artist ================

    override fun getMediaItems(artist: Artist): PagedData<MediaItemsContainer> {
        return PagedData.Single {
            withContext(Dispatchers.IO) {
                val similarUrl = userCredentials.urlBuilder().apply {
                    addPathSegment("Artists")
                    addPathSegment(artist.id)
                    addPathSegment("Similar")
                    addQueryParameter("Limit", "10")
                }.build()
                val similarItems = client.newCall(
                    makeApiRequest(userCredentials, similarUrl),
                ).execute().parseAs<ItemsListDto<ArtistDto>>()

                val (name, sortOrder, sortBy) = discSorting.split(",", limit = 3)

                listOf(
                    trackEndpoint.getItemsContainer(
                        containerName = "$name Songs".trim(),
                        sortBy = sortBy,
                        sortOrder = sortOrder,
                        fields = "ParentId",
                        limit = 15,
                        loadMore = true,
                    ) {
                        addQueryParameter("ArtistIds", artist.id)
                    },
                    albumEndpoint.getItemsContainer(
                        containerName = "$name Albums".trim(),
                        sortBy = sortBy,
                        sortOrder = sortOrder,
                        limit = 15,
                        loadMore = true,
                    ) {
                        addQueryParameter("ArtistIds", artist.id)
                    },
                    similarItems.toMediaItemsContainer(
                        "Similar artists",
                        userCredentials.serverUrl,
                    ),
                )
            }
        }
    }

    override suspend fun loadArtist(small: Artist): Artist {
        return artistEndpoint.loadArtist(small)
    }

    // ================ Tracks ================

    override fun getMediaItems(track: Track): PagedData<MediaItemsContainer> {
        return PagedData.Single {
            withContext(Dispatchers.IO) {
                val artists = track.artists.take(ARTISTS_PER_TRACK).map {
                    MediaItemsContainer.Item(loadArtist(it).toMediaItem())
                }
                val albums = track.album?.let {
                    listOf(MediaItemsContainer.Item(loadAlbum(it).toMediaItem()))
                } ?: emptyList()

                artists + albums
            }
        }
    }

    override suspend fun getStreamableAudio(streamable: Streamable): StreamableAudio {
        return trackEndpoint.getStreamable(streamable)
    }

    override suspend fun getStreamableVideo(streamable: Streamable): StreamableVideo {
        throw UnsupportedOperationException()
    }

    override suspend fun loadTrack(track: Track): Track {
        return trackEndpoint.loadTrack(track)
    }

    // ================ Search ================

    override fun searchFeed(query: String?, tab: Tab?): PagedData<MediaItemsContainer> {
        require(userCredentials.userId.isNotEmpty()) { throw ClientException.LoginRequired() }

        return when (tab?.id) {
            "tracks" -> getTrackSearch(trackEndpoint, query)
            "albums" -> getAlbumSearch(albumEndpoint, query)
            "artists" -> getArtistSearch(artistEndpoint, query)
            else -> throw IllegalArgumentException("Invalid tab")
        }
    }

    override suspend fun searchTabs(query: String?): List<Tab> {
        require(userCredentials.userId.isNotEmpty()) { throw ClientException.LoginRequired() }
        return listOf(
            Tab("tracks", "Tracks"),
            Tab("albums", "Albums"),
            Tab("artists", "Artists"),
        )
    }

    // ============= Quick Search =============

    override suspend fun quickSearch(query: String?): List<QuickSearchItem> {
        return emptyList()
    }

    @Suppress("EmptyFunctionBlock")
    override suspend fun deleteSearchHistory(query: QuickSearchItem.SearchQueryItem) { }

    // =============== Library ================

    override suspend fun getLibraryTabs(): List<Tab> {
        require(userCredentials.userId.isNotEmpty()) { throw ClientException.LoginRequired() }
        return listOf(
            Tab("all", "All"),
            Tab("playlists", "Playlists"),
            Tab("albums", "Albums"),
            Tab("tracks", "Tracks"),
            Tab("artists", "Artists"),
            Tab("history", "History"),
        )
    }

    override fun getLibraryFeed(tab: Tab?): PagedData<MediaItemsContainer> {
        require(userCredentials.userId.isNotEmpty()) { throw ClientException.LoginRequired() }

        return when (tab?.id) {
            "all" -> getLibraryAll(albumEndpoint, artistEndpoint, playlistEndpoint, trackEndpoint)
            "playlists" -> getLibraryPlaylists(playlistEndpoint)
            "albums" -> getLibraryAlbums(albumEndpoint)
            "tracks" -> getLibraryTracks(trackEndpoint)
            "artists" -> getLibraryArtists(artistEndpoint)
            "history" -> getLibraryHistory(trackEndpoint)
            else -> throw IllegalArgumentException("Invalid tab")
        }
    }

    // =============== Playlist ===============

    override fun getMediaItems(playlist: Playlist): PagedData<MediaItemsContainer> {
        return PagedData.Single { emptyList() }
    }

    override suspend fun loadPlaylist(playlist: Playlist): Playlist {
        return playlistEndpoint.loadPlaylist(playlist)
    }

    override fun loadTracks(playlist: Playlist): PagedData<Track> {
        return if (playlist.id == "radio") {
            radioTracks
        } else {
            playlistEndpoint.loadTracks(playlist)
        }
    }

    override suspend fun likeTrack(track: Track, liked: Boolean): Boolean {
        return playlistEndpoint.likeTrack(track, liked)
    }

    override suspend fun listEditablePlaylists(): List<Playlist> {
        return playlistEndpoint.getItemsList(
            sortBy = "SortName",
            limit = 15,
        ).items.map { it.toPlaylist(userCredentials.serverUrl) }
    }

    // ============ Edit Playlist =============

    override suspend fun createPlaylist(title: String, description: String?): Playlist {
        return playlistEndpoint.createPlaylist(title, description)
    }

    override suspend fun deletePlaylist(playlist: Playlist) {
        playlistEndpoint.deletePlaylist(playlist)
    }

    override suspend fun addTracksToPlaylist(
        playlist: Playlist,
        tracks: List<Track>,
        index: Int,
        new: List<Track>,
    ) {
        playlistEndpoint.addToPlaylist(playlist, new)
    }

    override suspend fun editPlaylistMetadata(
        playlist: Playlist,
        title: String,
        description: String?,
    ) {
        playlistEndpoint.editPlaylistMetadata(playlist, title, description)
    }

    // TODO(secozzi): Check this
    override fun editPlaylistCover(playlist: Playlist, cover: File?) {
        playlistEndpoint.editPlaylistCover(playlist, cover)
    }

    override suspend fun moveTrackInPlaylist(
        playlist: Playlist,
        tracks: List<Track>,
        fromIndex: Int,
        toIndex: Int,
    ) {
        playlistEndpoint.moveTrackInPlaylist(playlist, tracks, fromIndex, toIndex)
    }

    override suspend fun removeTracksFromPlaylist(
        playlist: Playlist,
        tracks: List<Track>,
        indexes: List<Int>,
    ) {
        playlistEndpoint.removeTracksFromPlaylist(playlist, tracks, indexes)
    }

    // ================ Radio =================

    private lateinit var radioTracks: PagedData<Track>

    private fun getRadio(itemId: String, itemName: String, cover: ImageHolder?): Playlist {
        radioTracks = trackEndpoint.getRadio(itemId)

        return Playlist(
            id = "radio",
            title = "$itemName Radio",
            isEditable = false,
            cover = cover,
        )
    }

    override suspend fun radio(album: Album): Playlist {
        return getRadio(album.id, album.title, album.cover)
    }

    override suspend fun radio(artist: Artist): Playlist {
        return getRadio(artist.id, artist.name, artist.cover)
    }

    override suspend fun radio(playlist: Playlist): Playlist {
        return getRadio(playlist.id, playlist.title, playlist.cover)
    }

    override suspend fun radio(track: Track): Playlist {
        return getRadio(track.id, track.title, track.cover)
    }

    override suspend fun radio(user: User): Playlist {
        throw UnsupportedOperationException()
    }

    // =============== Tracking ===============

    // TODO(secozzi): Check this
    override suspend fun onMarkAsPlayed(clientId: String, context: EchoMediaItem?, track: Track) {
        val url = userCredentials.urlBuilder().apply {
            addPathSegment("Users")
            addPathSegment(userCredentials.userId)
            addPathSegment("PlayedItems")
            addPathSegment(track.id)
        }.build()

        val headers = getHeaders(userCredentials)
        client.newCall(
            POST(url, headers = headers),
        ).execute()
    }

    override suspend fun onStartedPlaying(clientId: String, context: EchoMediaItem?, track: Track) {
        val url = userCredentials.urlBuilder().apply {
            addPathSegment("Sessions")
            addPathSegment("Playing")
        }.build()

        val body = buildJsonObject {
            put("ItemId", track.id)
        }.toRequestBody()

        client.newCall(
            makeApiRequest(userCredentials, url, body),
        ).execute()
    }

    override suspend fun onStoppedPlaying(clientId: String, context: EchoMediaItem?, track: Track) {
        val url = userCredentials.urlBuilder().apply {
            addPathSegment("Sessions")
            addPathSegment("Playing")
            addPathSegment("Stopped")
        }.build()

        val body = buildJsonObject {
            put("ItemId", track.id)
        }.toRequestBody()

        client.newCall(
            makeApiRequest(userCredentials, url, body),
        ).execute()
    }

    // =============== Settings ===============

    override val settingItems: List<Setting> = listOf(
        SettingList(
            title = "Artist discography sorting",
            key = "discography_sorting",
            entryTitles = listOf("Name", "Play Count", "Recently Added", "Release Date"),
            // (Display title,SortOrder,SortKey)
            entryValues = listOf(
                ",Ascending,SortName",
                "Most Played,Descending,PlayCount",
                "Recent,Descending,DateCreated,SortName",
                "New,Descending,ProductionYear,PremiereDate,SortName",
            ),
            defaultEntryIndex = 0,
        ),
    )

    private lateinit var setting: Settings
    override fun setSettings(settings: Settings) {
        setting = settings
    }

    private val discSorting
        get() = setting.getString("discography_sorting") ?: ",Ascending,SortName"

    // ================ Login =================

    override val loginInputFields: List<LoginClient.InputField> = listOf(
        LoginClient.InputField(
            key = "server_url",
            label = "Server address",
            isRequired = true,
            isPassword = false,
        ),
        LoginClient.InputField(
            key = "username",
            label = "Username",
            isRequired = true,
            isPassword = false,
        ),
        LoginClient.InputField(
            key = "password",
            label = "Password",
            isRequired = false,
            isPassword = true,
        ),
    )

    override suspend fun onLogin(data: Map<String, String?>): List<User> {
        Logger.getLogger("SOMETHING-data").log(Level.INFO, data.toString())
        val serverUrl = data["server_url"]!!

        val body = buildJsonObject {
            put("Username", data["username"]!!)
            put("Pw", data["password"]!!)
        }.toRequestBody()

        val headers = Headers.Builder().apply {
            add(
                "Accept",
                "application/json, application/octet-stream;q=0.9, */*;q=0.8",
            )
            add(
                "Authorization",
                """MediaBrowser Client="Echo", """ +
                    """Device="Echo Extension", """ +
                    """DeviceId="${randomString()}", """ +
                    """Version="1.0.0"""",
            )
        }.build()

        val url = serverUrl.toHttpUrl().newBuilder().apply {
            addPathSegment("Users")
            addPathSegment("AuthenticateByName")
        }.build()

        val responseData = client.newCall(
            POST(url, headers, body),
        ).execute().parseAs<LoginDto>()

        val user = User(
            id = responseData.user.id,
            name = responseData.user.name,
            cover = "$serverUrl/Users/${responseData.user.id}/Images/Primary".toImageHolder(),
            extras = mapOf(
                "access_token" to responseData.accessToken,
                "server_url" to serverUrl,
            ),
        )

        return listOf(user)
    }

    override suspend fun onSetLoginUser(user: User?) {
        require(user != null) { throw ClientException.LoginRequired() }
        userCredentials = UserCredentials(
            user.id,
            user.extras["access_token"]!!,
            user.extras["server_url"]!!,
        )
        albumEndpoint.userCredentials = userCredentials
        artistEndpoint.userCredentials = userCredentials
        playlistEndpoint.userCredentials = userCredentials
        trackEndpoint.userCredentials = userCredentials
    }

    // TODO(secozzi): implement
    override suspend fun getCurrentUser(): User? {
        Logger.getLogger("SOMETHING").log(Level.INFO, userCredentials.toString())
        return null
    }

    // ================ Utils =================

    @Suppress("UnusedPrivateProperty")
    private fun randomString(length: Int = 16): String {
        val charPool = ('a'..'z') + ('0'..'9')

        return buildString(length) {
            for (i in 0 until length) {
                append(charPool.random())
            }
        }
    }

    companion object {
        private const val ARTISTS_PER_TRACK = 5
    }
}
