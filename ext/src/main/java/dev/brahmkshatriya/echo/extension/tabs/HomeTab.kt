package dev.brahmkshatriya.echo.extension.tabs

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.extension.JellyfinExtension
import dev.brahmkshatriya.echo.extension.withIO

context(ext: JellyfinExtension)
fun createHomeFeed(): PagedData<Shelf> {
    return PagedData.Single {
        withIO {
            listOf(
                ext.api.getTrackList(
                    shelfTitle = "Most played",
                    sortBy = "PlayCount,SortName",
                ),
                ext.api.getAlbumList(
                    shelfTitle = "Newly added releases",
                    sortBy = "DateCreated,SortName",
                ),
                ext.api.getArtistList(
                    shelfTitle = "Favorite artists",
                    sortBy = "PlayCount,SortName",
                ),
                ext.api.getAlbumList(
                    shelfTitle = "Explore from your library",
                    sortBy = "Random,SortName",
                    sortOrder = "Ascending",
                ),
            )
        }
    }
}
