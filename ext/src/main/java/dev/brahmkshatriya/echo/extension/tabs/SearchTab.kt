package dev.brahmkshatriya.echo.extension.tabs

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.extension.JellyfinExtension

context(ext: JellyfinExtension)
fun createAllSearchFeed(query: String): PagedData<Shelf> {
    return PagedData.Single {
        listOf(
            ext.api.getAlbumShelf(
                query = query,
                shelfTitle = "Albums",
                sortBy = "SortName",
                sortOrder = "Ascending",
            ),
            ext.api.getArtistShelf(
                query = query,
                shelfTitle = "Artists",
                sortBy = "SortName",
                sortOrder = "Ascending",
            ),
            ext.api.getPlaylistShelf(
                query = query,
                shelfTitle = "Playlists",
                sortBy = "SortName",
                sortOrder = "Ascending",
            ),
            ext.api.getTrackShelf(
                query = query,
                shelfTitle = "Tracks",
                sortBy = "SortName",
                sortOrder = "Ascending",
            ),
        )
    }
}
