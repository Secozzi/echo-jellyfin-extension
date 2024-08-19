package dev.brahmkshatriya.echo.extension.tabs

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.extension.endpoints.AlbumEndpoint
import dev.brahmkshatriya.echo.extension.endpoints.ArtistEndpoint
import dev.brahmkshatriya.echo.extension.endpoints.PlaylistEndpoint
import dev.brahmkshatriya.echo.extension.endpoints.TrackEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun getLibraryAll(
    albumEndpoint: AlbumEndpoint,
    artistEndpoint: ArtistEndpoint,
    playlistEndpoint: PlaylistEndpoint,
    trackEndpoint: TrackEndpoint,
): PagedData<MediaItemsContainer> {
    return PagedData.Single {
        withContext(Dispatchers.IO) {
            listOf(
                playlistEndpoint.getItemsContainer(
                    containerName = "Playlists",
                    sortBy = "SortName",
                    limit = 15,
                ),
                albumEndpoint.getItemsContainer(
                    containerName = "Albums",
                    sortBy = "SortName",
                    limit = 15,
                ),
                trackEndpoint.getItemsContainer(
                    containerName = "Tracks",
                    sortBy = "Album,SortName",
                    fields = "ParentId",
                    limit = 15,
                ),
                artistEndpoint.getItemsContainer(
                    containerName = "Artists",
                    sortBy = "SortName",
                    limit = 15,
                ),
                trackEndpoint.getItemsContainer(
                    containerName = "History",
                    sortBy = "DatePlayed,SortName",
                    sortOrder = "Descending",
                    fields = "ParentId",
                    limit = 15,
                ),
            )
        }
    }
}

fun getLibraryPlaylists(playlistEndpoint: PlaylistEndpoint): PagedData<MediaItemsContainer> {
    return playlistEndpoint.getContinuousItemList(
        sortBy = "SortName",
        limit = 15,
    )
}

fun getLibraryAlbums(albumEndpoint: AlbumEndpoint): PagedData<MediaItemsContainer> {
    return albumEndpoint.getContinuousItemList(
        sortBy = "SortName",
        limit = 15,
    )
}

fun getLibraryTracks(trackEndpoint: TrackEndpoint): PagedData<MediaItemsContainer> {
    return trackEndpoint.getContinuousItemList(
        sortBy = "Album,SortName",
        fields = "ParentId",
        limit = 15,
    )
}

fun getLibraryArtists(artistEndpoint: ArtistEndpoint): PagedData<MediaItemsContainer> {
    return artistEndpoint.getContinuousItemList(
        sortBy = "SortName",
        limit = 15,
    )
}

fun getLibraryHistory(trackEndpoint: TrackEndpoint): PagedData<MediaItemsContainer> {
    return trackEndpoint.getContinuousItemList(
        sortBy = "DatePlayed,SortName",
        sortOrder = "Descending",
        fields = "ParentId",
        limit = 15,
    )
}
