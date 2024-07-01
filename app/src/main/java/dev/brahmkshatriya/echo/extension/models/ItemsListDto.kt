package dev.brahmkshatriya.echo.extension.models

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import kotlinx.serialization.Serializable

interface MediaItem {
    fun toMediaItem(serverUrl: String): EchoMediaItem
}

@Serializable
class ItemsListDto<T : MediaItem>(
    val items: List<T>,
    val totalRecordCount: Int,
) {
    fun toMediaItemsContainer(
        name: String,
        serverUrl: String,
        more: PagedData<EchoMediaItem>? = null,
    ): MediaItemsContainer {
        return MediaItemsContainer.Category(
            title = name,
            list = this.items.map { it.toMediaItem(serverUrl) },
            more = more,
        )
    }
}
