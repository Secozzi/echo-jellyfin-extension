package dev.brahmkshatriya.echo.extension.tabs

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.extension.endpoints.AlbumEndpoint
import dev.brahmkshatriya.echo.extension.endpoints.ArtistEndpoint
import dev.brahmkshatriya.echo.extension.endpoints.TrackEndpoint

fun getTrackSearch(trackEndpoint: TrackEndpoint, query: String?): PagedData<MediaItemsContainer> {
    return trackEndpoint.getContinuousItemList(
        sortBy = "DateCreated,SortName",
        sortOrder = "Descending",
        limit = 50,
        fields = "ParentId",
    ) {
        addQueryParameter("SearchTerm", query ?: "")
    }
}

fun getAlbumSearch(albumEndpoint: AlbumEndpoint, query: String?): PagedData<MediaItemsContainer> {
    return albumEndpoint.getContinuousItemList(
        sortBy = "SortName",
        limit = 50,
    ) {
        addQueryParameter("SearchTerm", query ?: "")
    }
}

fun getArtistSearch(artistEndpoint: ArtistEndpoint, query: String?): PagedData<MediaItemsContainer> {
    return artistEndpoint.getContinuousItemList(
        sortBy = "SortName,Name",
        limit = 50,
    ) {
        addQueryParameter("SearchTerm", query ?: "")
    }
}
