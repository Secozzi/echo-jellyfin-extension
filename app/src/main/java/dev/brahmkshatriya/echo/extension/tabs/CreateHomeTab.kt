package dev.brahmkshatriya.echo.extension.tabs

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.extension.endpoints.AlbumEndpoint
import dev.brahmkshatriya.echo.extension.endpoints.TrackEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun getHomeFeed(
    albumEndpoint: AlbumEndpoint,
    trackEndpoint: TrackEndpoint,
): PagedData<MediaItemsContainer> {
    return PagedData.Single {
        withContext(Dispatchers.IO) {
            listOf(
                trackEndpoint.getItemsContainer(
                    containerName = "Most played",
                    sortBy = "PlayCount,SortName",
                    sortOrder = "Descending",
                    fields = "ParentId,AlbumPrimaryImageTag",
                    limit = 15,
                    loadMore = true,
                ),
                trackEndpoint.getItemsContainer(
                    containerName = "Favorites",
                    sortBy = "DateCreated,SortName",
                    sortOrder = "Descending",
                    fields = "ParentId,AlbumPrimaryImageTag",
                    limit = 15,
                    loadMore = true,
                ) {
                    addQueryParameter("IsFavorite", "true")
                },
                albumEndpoint.getItemsContainer(
                    containerName = "Explore from your library",
                    sortBy = "Random,SortName",
                    limit = 15,
                    loadMore = true,
                ),
                albumEndpoint.getItemsContainer(
                    containerName = "Newly added albums",
                    sortBy = "DateCreated,SortName",
                    sortOrder = "Descending",
                    limit = 15,
                    loadMore = true,
                ),
            )
        }
    }
}
