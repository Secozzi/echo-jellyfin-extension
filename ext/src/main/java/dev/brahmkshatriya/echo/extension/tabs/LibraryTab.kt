package dev.brahmkshatriya.echo.extension.tabs

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.extension.JellyfinExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

context(ext: JellyfinExtension)
fun createAllLibraryFeed(): PagedData<Shelf> {
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
    }
}

context(ext: JellyfinExtension)
fun createFavoriteLibraryFeed(): PagedData<Shelf> {
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
    }
}
