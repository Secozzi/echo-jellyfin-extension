package dev.brahmkshatriya.echo.extension.tabs

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.extension.JellyfinExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

context(ext: JellyfinExtension)
fun createAllSearchFeed(query: String): PagedData<Shelf> {
    return PagedData.Single {
        withContext(Dispatchers.IO) {
            listOf(
                async {
                    ext.api.getAlbumShelf(
                        shelfTitle = "Albums",
                        sortBy = "SortName",
                        sortOrder = "Ascending",
                        extraParams = mapOf("SearchTerm" to query),
                    )
                },
                async {
                    ext.api.getArtistShelf(
                        shelfTitle = "Artists",
                        sortBy = "SortName",
                        sortOrder = "Ascending",
                        extraParams = mapOf("SearchTerm" to query),
                    )
                },
                async {
                    ext.api.getPlaylistShelf(
                        shelfTitle = "Playlists",
                        sortBy = "SortName",
                        sortOrder = "Ascending",
                        extraParams = mapOf("SearchTerm" to query),
                    )
                },
                async {
                    ext.api.getTrackShelf(
                        shelfTitle = "Tracks",
                        sortBy = "SortName",
                        sortOrder = "Ascending",
                        extraParams = mapOf("SearchTerm" to query),
                    )
                },
            ).awaitAll()
        }
    }
}
