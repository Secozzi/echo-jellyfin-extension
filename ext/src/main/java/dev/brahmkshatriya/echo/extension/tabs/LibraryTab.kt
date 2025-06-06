package dev.brahmkshatriya.echo.extension.tabs

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.extension.JellyfinExtension

context(ext: JellyfinExtension)
fun createAllLibraryFeed(): PagedData<Shelf> {
    return PagedData.Single {
        listOf(
            ext.api.getPlaylistShelf(
                shelfTitle = "Playlists",
                sortBy = "SortName",
                sortOrder = "Ascending",
            ),
            ext.api.getAlbumShelf(
                shelfTitle = "Albums",
                sortBy = "SortName",
                sortOrder = "Ascending",
            ),
            ext.api.getArtistShelf(
                shelfTitle = "Artists",
                sortBy = "SortName",
                sortOrder = "Ascending",
            ),
            ext.api.getTrackShelf(
                shelfTitle = "Tracks",
                sortBy = "SortName",
                sortOrder = "Ascending",
            ),
        )
    }
}
