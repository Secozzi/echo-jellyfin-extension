package dev.brahmkshatriya.echo.extension.tabs

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.extension.JellyfinExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

context(ext: JellyfinExtension)
fun createHomeFeed(): PagedData<Shelf> {
    return PagedData.Single {
        withContext(Dispatchers.IO) {
            listOf(
                async {
                    ext.api.getTrackShelf(
                        shelfTitle = "Most played",
                        sortBy = "PlayCount,SortName",
                    )
                },
                async {
                    ext.api.getTrackShelf(
                        shelfTitle = "Favorite tracks",
                        sortBy = "DatePlayed,SortName",
                        sortOrder = "Descending",
                        extraParams = mapOf("IsFavorite" to "true"),
                    )
                },
                async {
                    ext.api.getAlbumShelf(
                        shelfTitle = "Newly added releases",
                        sortBy = "DateCreated,SortName",
                    )
                },
                async {
                    ext.api.getArtistShelf(
                        shelfTitle = "Favorite artists",
                        sortBy = "PlayCount,SortName",
                    )
                },
                async {
                    ext.api.getAlbumShelf(
                        shelfTitle = "Explore from your library",
                        sortBy = "Random,SortName",
                        sortOrder = "Ascending",
                    )
                },
            ).awaitAll()
        }
    }
}
