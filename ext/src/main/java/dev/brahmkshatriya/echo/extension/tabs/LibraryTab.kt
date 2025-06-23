package dev.brahmkshatriya.echo.extension.tabs

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.extension.JellyfinExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

context(ext: JellyfinExtension)
fun createAllLibraryFeed(): Feed {
    return PagedData.Single {
        withContext(Dispatchers.IO) {
            listOf(
                async {
                    ext.api.getAlbumShelf(
                        shelfTitle = "Albums",
                        sortBy = "SortName",
                        sortOrder = "Ascending",
                    )
                },
                async {
                    ext.api.getArtistShelf(
                        shelfTitle = "Artists",
                        sortBy = "SortName",
                        sortOrder = "Ascending",
                    )
                },
                async {
                    ext.api.getPlaylistShelf(
                        shelfTitle = "Playlists",
                        sortBy = "SortName",
                        sortOrder = "Ascending",
                    )
                },
                async {
                    ext.api.getTrackShelf(
                        shelfTitle = "Tracks",
                        sortBy = "SortName",
                        sortOrder = "Ascending",
                    )
                },
            ).awaitAll()
        }
    }.toFeed()
}

context(ext: JellyfinExtension)
fun createFavoriteLibraryFeed(): Feed {
    return PagedData.Single {
        withContext(Dispatchers.IO) {
            listOf(
                // TODO: add back when jellyfin sorts it correctly
                // async {
                //     ext.api.getAlbumShelf(
                //         shelfTitle = "Albums",
                //         sortBy = "DatePlayed,SortName",
                //         sortOrder = "Descending",
                //         extraParams = mapOf("IsFavorite" to "true"),
                //     )
                // },
                async {
                    ext.api.getArtistShelf(
                        shelfTitle = "Artists",
                        sortBy = "DatePlayed,SortName",
                        sortOrder = "Descending",
                        extraParams = mapOf("IsFavorite" to "true"),
                    )
                },
                async {
                    ext.api.getPlaylistShelf(
                        shelfTitle = "Playlists",
                        sortBy = "DatePlayed,SortName",
                        sortOrder = "Descending",
                        extraParams = mapOf("IsFavorite" to "true"),
                    )
                },
                async {
                    ext.api.getTrackShelf(
                        shelfTitle = "Tracks",
                        sortBy = "DatePlayed,SortName",
                        sortOrder = "Descending",
                        extraParams = mapOf("IsFavorite" to "true"),
                    )
                },
            ).awaitAll()
        }
    }.toFeed()
}
