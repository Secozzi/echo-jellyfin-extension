package dev.brahmkshatriya.echo.extension.tabs

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.extension.JellyfinExtension

context(ext: JellyfinExtension)
fun createHomeFeed(): PagedData<Shelf> {
    return PagedData.Single {
        listOf(
            ext.api.getTrackShelf(
                shelfTitle = "Most played",
                sortBy = "PlayCount,SortName",
            ),
            ext.api.getAlbumShelf(
                shelfTitle = "Newly added releases",
                sortBy = "DateCreated,SortName",
            ),
            ext.api.getArtistShelf(
                shelfTitle = "Favorite artists",
                sortBy = "PlayCount,SortName",
            ),
            ext.api.getAlbumShelf(
                shelfTitle = "Explore from your library",
                sortBy = "Random,SortName",
                sortOrder = "Ascending",
            ),
        )
    }
}
