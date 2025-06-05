package dev.brahmkshatriya.echo.extension.dto

import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import kotlinx.serialization.Serializable

interface MediaItem {
    fun toMediaItem(serverUrl: String): EchoMediaItem
}

@Serializable
data class ItemListDto<T : MediaItem>(
    val items: List<T>,
    val totalRecordCount: Int,
    val startIndex: Int,
)
