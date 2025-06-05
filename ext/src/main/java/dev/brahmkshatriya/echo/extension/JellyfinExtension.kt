package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.clients.SearchFeedClient
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.QuickSearchItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.extension.tabs.createHomeFeed

class JellyfinExtension : ExtensionClient, LoginClient.CustomInput, HomeFeedClient, SearchFeedClient {

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
            Tab("tracks", "Tracks"),
            Tab("albums", "Albums"),
            Tab("artists", "Artists"),
        )
    }

    override fun searchFeed(
        query: String,
        tab: Tab?,
    ): PagedData<Shelf> {
        saveQueryToHistory(query)

        return when (tab?.id) {
            "tracks" -> api.getTrackSearch(query)
            "albums" -> api.getAlbumSearch(query)
            "artists" -> api.getArtistSearch(query)
            else -> throw IllegalArgumentException("Invalid search tab")
        }
    }

    // ================ Utils =================

    companion object {
        const val SETTINGS_DEVICE_ID_KEY = "device_id"
        const val SETTINGS_HISTORY_KEY = "search_history"
    }
}
