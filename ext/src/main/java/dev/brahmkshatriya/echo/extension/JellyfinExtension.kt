package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.ArtistFollowClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.LibraryFeedClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.PlaylistEditClient
import dev.brahmkshatriya.echo.common.clients.SearchFeedClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.clients.TrackLikeClient
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.QuickSearchItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.extension.tabs.createAllLibraryFeed
import dev.brahmkshatriya.echo.extension.tabs.createAllSearchFeed
import dev.brahmkshatriya.echo.extension.tabs.createHomeFeed

class JellyfinExtension :
    ExtensionClient,
    LoginClient.CustomInput,
    HomeFeedClient,
    SearchFeedClient,
    AlbumClient,
    ArtistClient,
    ArtistFollowClient,
    PlaylistClient,
    PlaylistEditClient,
    TrackClient,
    TrackLikeClient,
    LibraryFeedClient {

    val api by lazy { JellyfinApi() }

    // =============== Settings ===============

    override suspend fun onExtensionSelected() {}

    override val settingItems: List<Setting> = emptyList()

    private lateinit var setting: Settings

    override fun setSettings(settings: Settings) {
        setting = settings
    }

    val deviceId: String
        get() = setting.getString(SETTINGS_DEVICE_ID_KEY).orEmpty().ifBlank {
            randomString().also { setting.putString(SETTINGS_DEVICE_ID_KEY, it) }
        }

    // ================ Login =================

    enum class LoginType {
        UserPass,
    }

    override val forms: List<LoginClient.Form> = listOf(
        LoginClient.Form(
            key = LoginType.UserPass.name,
            label = "Username and Password",
            icon = LoginClient.InputField.Type.Username,
            inputFields = listOf(
                LoginClient.InputField(
                    type = LoginClient.InputField.Type.Url,
                    key = "address",
                    label = "Address",
                    isRequired = true,
                ),
                LoginClient.InputField(
                    type = LoginClient.InputField.Type.Username,
                    key = "username",
                    label = "Username",
                    isRequired = true,
                ),
                LoginClient.InputField(
                    type = LoginClient.InputField.Type.Password,
                    key = "password",
                    label = "Password",
                    isRequired = false,
                ),
            ),
        ),
    )

    override suspend fun onLogin(
        key: String,
        data: Map<String, String?>,
    ): List<User> {
        return when (LoginType.valueOf(key)) {
            LoginType.UserPass -> {
                api.onLogin(data, deviceId)
            }
        }
    }

    override suspend fun onSetLoginUser(user: User?) {
        api.setUser(user)
    }

    override suspend fun getCurrentUser(): User? {
        return api.getUser()
    }

    // ============== Home Feed ===============

    override suspend fun getHomeTabs(): List<Tab> {
        return emptyList()
    }

    override fun getHomeFeed(tab: Tab?): PagedData<Shelf> {
        return createHomeFeed()
    }

    // ================ Search ================

    private var searchHistory: List<String>
        get() = setting.getString(SETTINGS_HISTORY_KEY)
            ?.split(",")?.distinct()?.filter(String::isNotBlank)?.take(5)
            ?: emptyList()
        set(value) = setting.putString(SETTINGS_HISTORY_KEY, value.joinToString(","))

    private fun saveQueryToHistory(query: String) {
        val history = searchHistory.toMutableList()
        history.add(0, query)
        searchHistory = history
    }

    override suspend fun quickSearch(query: String): List<QuickSearchItem> {
        return if (query.isBlank()) {
            searchHistory.map { QuickSearchItem.Query(it, true) }
        } else {
            emptyList()
        }
    }

    override suspend fun deleteQuickSearch(item: QuickSearchItem) {
        searchHistory -= item.title
    }

    override suspend fun searchTabs(query: String): List<Tab> {
        return listOf(
            Tab("all", "All"),
            Tab("albums", "Albums"),
            Tab("artists", "Artists"),
            Tab("playlists", "Playlists"),
            Tab("tracks", "Tracks"),
        )
    }

    override fun searchFeed(
        query: String,
        tab: Tab?,
    ): PagedData<Shelf> {
        saveQueryToHistory(query)

        return when (tab?.id) {
            "all" -> createAllSearchFeed(query)
            "albums" -> api.getAlbumPage(query)
            "artists" -> api.getArtistPage(query)
            "playlists" -> api.getPlaylistPage(query)
            "tracks" -> api.getTrackPage(query)
            else -> throw IllegalArgumentException("Invalid search tab")
        }
    }

    // =============== Library ================

    override suspend fun getLibraryTabs(): List<Tab> {
        return listOf(
            Tab("all", "All"),
            Tab("history", "History"),
            Tab("albums", "Albums"),
            Tab("artists", "Artists"),
            Tab("playlists", "Playlists"),
            Tab("tracks", "Tracks"),
        )
    }

    override fun getLibraryFeed(tab: Tab?): PagedData<Shelf> {
        return when (tab?.id) {
            "all" -> createAllLibraryFeed()
            "history" -> api.getTrackPage(sortBy = "DatePlayed,SortName", sortOrder = "Descending")
            "albums" -> api.getAlbumPage(sortBy = "SortName", sortOrder = "Ascending")
            "artists" -> api.getArtistPage(sortBy = "SortName", sortOrder = "Ascending")
            "playlists" -> api.getPlaylistPage(sortBy = "SortName", sortOrder = "Ascending")
            "tracks" -> api.getTrackPage(sortBy = "SortName", sortOrder = "Ascending")
            else -> throw IllegalArgumentException("Invalid library tab")
        }
    }

    // ================ Album =================

    override suspend fun loadAlbum(album: Album): Album {
        return api.getAlbum(album)
    }

    override fun loadTracks(album: Album): PagedData<Track> {
        return api.getAlbumTracks(album)
    }

    override fun getShelves(album: Album): PagedData<Shelf> {
        return PagedData.Single {
            listOfNotNull(
                album.artists.firstOrNull()?.let {
                    api.getArtistAlbums(
                        artist = it,
                        shelfTitle = "More from this artist",
                        sortBy = "SortName",
                        sortOrder = "Descending",
                    )
                },
            )
        }
    }

    // ================ Artist ================

    override suspend fun loadArtist(artist: Artist): Artist {
        return api.getArtist(artist)
    }

    override fun getShelves(artist: Artist): PagedData<Shelf> {
        return PagedData.Single {
            listOf(
                api.getArtistAlbums(
                    artist = artist,
                    shelfTitle = "Recent releases",
                    sortBy = "ProductionYear,PremiereDate,SortName",
                    sortOrder = "Descending",
                    limit = 15,
                ),
                api.getSimilarArtists(
                    artist = artist,
                    shelfTitle = "Related artists",
                ),
            )
        }
    }

    // ============ Follow Artist =============

    override suspend fun followArtist(artist: Artist, follow: Boolean) {
        api.followArtist(artist, follow)
    }

    // =============== Playlist ===============

    override suspend fun loadPlaylist(playlist: Playlist): Playlist {
        return api.getPlaylist(playlist)
    }

    override fun loadTracks(playlist: Playlist): PagedData<Track> {
        return api.getPlaylistTracks(playlist)
    }

    override fun getShelves(playlist: Playlist): PagedData<Shelf> {
        return PagedData.Single { emptyList() }
    }

    // ============ Edit Playlist =============

    override suspend fun listEditablePlaylists(track: Track?): List<Pair<Playlist, Boolean>> {
        return api.getPlaylists().map { it to true }
    }

    override suspend fun createPlaylist(
        title: String,
        description: String?,
    ): Playlist {
        return api.createPlaylist(title, description)
    }

    override suspend fun deletePlaylist(playlist: Playlist) {
        api.deletePlaylist(playlist)
    }

    override suspend fun editPlaylistMetadata(
        playlist: Playlist,
        title: String,
        description: String?,
    ) {
        api.editPlaylistMetadata(playlist.id, title, description)
    }

    override suspend fun addTracksToPlaylist(
        playlist: Playlist,
        tracks: List<Track>,
        index: Int,
        new: List<Track>,
    ) {
        api.addToPlaylist(playlist, new)
    }

    override suspend fun removeTracksFromPlaylist(
        playlist: Playlist,
        tracks: List<Track>,
        indexes: List<Int>,
    ) {
        api.removeFromPlaylist(playlist, tracks, indexes)
    }

    override suspend fun moveTrackInPlaylist(
        playlist: Playlist,
        tracks: List<Track>,
        fromIndex: Int,
        toIndex: Int,
    ) {
        api.moveInPlaylist(playlist, tracks, fromIndex, toIndex)
    }

    // ================ Track =================

    override suspend fun loadTrack(track: Track): Track {
        return api.getTrack(track)
    }

    override suspend fun loadStreamableMedia(
        streamable: Streamable,
        isDownload: Boolean,
    ): Streamable.Media {
        return api.getStreamableMedia(streamable)
    }

    override fun getShelves(track: Track): PagedData<Shelf> {
        return PagedData.Single { emptyList() }
    }

    // ============== Like Track ==============

    override suspend fun likeTrack(track: Track, isLiked: Boolean) {
        api.likeTrack(track, isLiked)
    }

    // ================ Utils =================

    companion object {
        const val SETTINGS_DEVICE_ID_KEY = "device_id"
        const val SETTINGS_HISTORY_KEY = "search_history"
    }
}
