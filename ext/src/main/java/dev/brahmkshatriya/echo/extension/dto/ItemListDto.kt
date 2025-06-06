package dev.brahmkshatriya.echo.extension.dto

import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import kotlinx.serialization.Serializable

interface MediaItem {
    fun toMediaItem(serverUrl: String): EchoMediaItem
}

fun MediaItem.toShelf(serverUrl: String): Shelf {
    return this.toMediaItem(serverUrl).toShelf()
}

@Serializable
data class ItemListDto<T : MediaItem>(
    val items: List<T>,
    val totalRecordCount: Int,
    val startIndex: Int,
)
