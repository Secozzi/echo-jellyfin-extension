package dev.brahmkshatriya.echo.extension.models

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface MediaItem {
    fun toMediaItem(serverUrl: String): EchoMediaItem
}

@Serializable
class ItemsListDto<T : MediaItem>(
    @SerialName("Items") val items: List<T>,
    @SerialName("TotalRecordCount") val itemCount: Int,
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
