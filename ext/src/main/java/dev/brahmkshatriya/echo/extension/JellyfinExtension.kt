package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.LibraryFeedClient
import dev.brahmkshatriya.echo.common.clients.LikeClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.clients.LyricsClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.PlaylistEditClient
import dev.brahmkshatriya.echo.common.clients.PlaylistEditCoverClient
import dev.brahmkshatriya.echo.common.clients.QuickSearchClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.SearchFeedClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.clients.TrackerClient
import dev.brahmkshatriya.echo.common.clients.TrackerMarkClient
import dev.brahmkshatriya.echo.common.helpers.ClientException
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Lyrics
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.QuickSearchItem
import dev.brahmkshatriya.echo.common.models.Radio
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.TrackDetails
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.extension.tabs.createAllLibraryFeed
import dev.brahmkshatriya.echo.extension.tabs.createAllSearchFeed
import dev.brahmkshatriya.echo.extension.tabs.createFavoriteLibraryFeed
import dev.brahmkshatriya.echo.extension.tabs.createHomeFeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.File

class JellyfinExtension :
    AlbumClient,
    ArtistClient,
    ExtensionClient,
    HomeFeedClient,
    LibraryFeedClient,
    LikeClient,
    LoginClient.CustomInput,
    LyricsClient,
    PlaylistClient,
    PlaylistEditClient,
    PlaylistEditCoverClient,
    QuickSearchClient,
    RadioClient,
    SearchFeedClient,
    TrackClient,
    TrackerMarkClient {

    val api by lazy { JellyfinApi() }

    // =============== Settings ===============

    override suspend fun onExtensionSelected() {}

    override suspend fun getSettingItems(): List<Setting> {
        return emptyList()
    }

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

    override fun setLoginUser(user: User?) {
        api.setUser(user)
    }

    override suspend fun getCurrentUser(): User? {
        return api.getUser()
    }

    // ============== Home Feed ===============

    override suspend fun loadHomeFeed(): Feed<Shelf> {
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

    override suspend fun loadSearchFeed(
        query: String,
    ): Feed<Shelf> {
        saveQueryToHistory(query)

        return Feed(
            listOf(
                Tab("all", "All"),
                Tab("albums", "Albums"),
                Tab("artists", "Artists"),
                Tab("playlists", "Playlists"),
                Tab("tracks", "Tracks"),
            ),
        ) { tab ->
            when (tab?.id) {
                "all" -> createAllSearchFeed(query)
                "albums" -> api.getAlbumPage(query)
                "artists" -> api.getArtistPage(query)
                "playlists" -> api.getPlaylistPage(query)
                "tracks" -> api.getTrackPage(query)
                else -> throw IllegalArgumentException("Invalid search tab")
            }
        }
    }

    // =============== Library ================

    override suspend fun loadLibraryFeed(): Feed<Shelf> {
        return Feed(
            listOf(
                Tab("all", "All"),
                Tab("history", "History"),
                Tab("favorites", "Favorites"),
                Tab("albums", "Albums"),
                Tab("artists", "Artists"),
                Tab("playlists", "Playlists"),
                Tab("tracks", "Tracks"),
            ),
        ) { tab ->
            when (tab?.id) {
                "all" -> createAllLibraryFeed()
                "history" -> api.getTrackPage(sortBy = "DatePlayed,SortName", sortOrder = "Descending")
                "favorites" -> createFavoriteLibraryFeed()
                "albums" -> api.getAlbumPage(sortBy = "SortName", sortOrder = "Ascending")
                "artists" -> api.getArtistPage(sortBy = "SortName", sortOrder = "Ascending")
                "playlists" -> api.getPlaylistPage(sortBy = "SortName", sortOrder = "Ascending")
                "tracks" -> api.getTrackPage(sortBy = "SortName", sortOrder = "Ascending")
                else -> throw IllegalArgumentException("Invalid library tab")
            }
        }
    }

    // ================ Album =================

    override suspend fun loadAlbum(album: Album): Album {
        return api.getAlbum(album)
    }

    override suspend fun loadTracks(album: Album): Feed<Track> {
        return api.getAlbumTracks(album).toFeed()
    }

    override suspend fun loadFeed(album: Album): Feed<Shelf>? {
        return album.artists.firstOrNull()?.let {
            listOf(
                api.getArtistAlbums(
                    artist = it,
                    shelfTitle = "More from this artist",
                    sortBy = "SortName",
                    sortOrder = "Descending",
                ),
            ).toFeed()
        }
    }

    // ================ Artist ================

    override suspend fun loadArtist(artist: Artist): Artist {
        return api.getArtist(artist)
    }

    override suspend fun loadFeed(artist: Artist): Feed<Shelf> {
        return withContext(Dispatchers.IO) {
            listOf(
                async {
                    api.getArtistAlbums(
                        artist = artist,
                        shelfTitle = "Discography",
                        sortBy = "SortName",
                        sortOrder = "Ascending",
                        limit = 15,
                    )
                },
                async {
                    api.getArtistAlbums(
                        artist = artist,
                        shelfTitle = "Recent releases",
                        sortBy = "ProductionYear,PremiereDate,SortName",
                        sortOrder = "Descending",
                        limit = 15,
                    )
                },
                async {
                    api.getSimilarArtists(
                        artist = artist,
                        shelfTitle = "Related artists",
                    )
                },
            ).awaitAll()
        }.toFeed()
    }

    // =============== Playlist ===============

    override suspend fun loadPlaylist(playlist: Playlist): Playlist {
        return api.getPlaylist(playlist)
    }

    override suspend fun loadTracks(playlist: Playlist): Feed<Track> {
        return api.getPlaylistTracks(playlist).toFeed()
    }

    override suspend fun loadFeed(playlist: Playlist): Feed<Shelf>? {
        return null
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

    // ========= Edit Playlist Cover ==========

    override suspend fun editPlaylistCover(playlist: Playlist, cover: File?) {
        api.updateCover(playlist, cover)
    }

    // ================ Track =================

    override suspend fun loadTrack(track: Track, isDownload: Boolean): Track {
        return api.getTrack(track)
    }

    override suspend fun loadStreamableMedia(
        streamable: Streamable,
        isDownload: Boolean,
    ): Streamable.Media {
        return api.getStreamableMedia(streamable)
    }

    override suspend fun loadFeed(track: Track): Feed<Shelf>? {
        return null
    }

    // =============== Like Item ==============

    override suspend fun isItemLiked(item: EchoMediaItem): Boolean {
        return api.isItemLiked(item)
    }

    override suspend fun likeItem(item: EchoMediaItem, shouldLike: Boolean) {
        api.likeItem(item, shouldLike)
    }

    // =============== Tracker ================

    private var currentTrackId: String? = null
    override suspend fun onTrackChanged(details: TrackDetails?) {
        details?.track?.id?.let {
            currentTrackId = it
            api.postPlaying(it)
        }
    }

    override suspend fun getMarkAsPlayedDuration(details: TrackDetails): Long? {
        return null
    }

    override suspend fun onMarkAsPlayed(details: TrackDetails) { }

    override suspend fun onPlayingStateChanged(details: TrackDetails?, isPlaying: Boolean) {
        if (details == null) {
            currentTrackId?.let { api.postStopped(it) }
        } else {
            api.postProgress(details.track.id, !isPlaying, details.currentPosition * TICKS_PER_MS)
        }
    }

    // ================ Lyrics ================

    override suspend fun searchTrackLyrics(
        clientId: String,
        track: Track,
    ): Feed<Lyrics> {
        return api.getLyrics(track).toFeed()
    }

    override suspend fun loadLyrics(lyrics: Lyrics): Lyrics {
        return lyrics
    }

    // ================ Radio =================

    override suspend fun radio(item: EchoMediaItem, context: EchoMediaItem?): Radio {
        return when (item) {
            is Artist -> Radio(id = item.id, title = "Instant Mix", extras = mapOf("type" to "Artists"))
            is Album -> Radio(id = item.id, title = "Instant Mix", extras = mapOf("type" to "Albums"))
            is Playlist -> Radio(id = item.id, title = "Instant Mix", extras = mapOf("type" to "Playlists"))
            is Track -> Radio(id = item.id, title = "Instant Mix", extras = mapOf("type" to "Songs"))
            is Radio -> throw ClientException.NotSupported("Shivam, why?")
        }
    }

    override suspend fun loadTracks(radio: Radio): Feed<Track> {
        return api.getRadioPage(radio.id, radio.extras["type"]!!).toFeed()
    }

    override suspend fun loadRadio(radio: Radio): Radio {
        return radio
    }

    // ================ Utils =================

    companion object {
        const val SETTINGS_DEVICE_ID_KEY = "device_id"
        const val SETTINGS_HISTORY_KEY = "search_history"
    }
}
