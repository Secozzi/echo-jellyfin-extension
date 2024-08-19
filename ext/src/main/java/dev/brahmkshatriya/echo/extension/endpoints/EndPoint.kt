package dev.brahmkshatriya.echo.extension.endpoints

import dev.brahmkshatriya.echo.common.helpers.Page
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.extension.UserCredentials
import dev.brahmkshatriya.echo.extension.createItemsUrl
import dev.brahmkshatriya.echo.extension.json
import dev.brahmkshatriya.echo.extension.makeApiRequest
import dev.brahmkshatriya.echo.extension.models.ItemsListDto
import dev.brahmkshatriya.echo.extension.models.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Response

abstract class EndPoint<T : MediaItem>(
    val client: OkHttpClient,
    private val itemType: String,
    private val serializer: KSerializer<T>,
) {
    lateinit var userCredentials: UserCredentials

    private fun <T> Response.parseAs(serializer: KSerializer<T>): T {
        return json.decodeFromString(serializer, body.string())
    }

    protected open fun createUrl(
        itemType: String,
        sortBy: String,
        fields: String? = null,
        limit: String = "15",
        sortOrder: String = "Ascending",
        startIndex: String = "0",
        builderBlock: (HttpUrl.Builder.() -> Unit) = {},
    ): HttpUrl {
        return createItemsUrl(userCredentials, itemType, sortBy, fields, limit, sortOrder, startIndex, builderBlock)
    }

    fun getItemsList(
        sortBy: String,
        sortOrder: String = "Ascending",
        fields: String? = null,
        limit: Int,
        builderBlock: (HttpUrl.Builder.() -> Unit) = {},
    ): ItemsListDto<T> {
        val url = createUrl(
            itemType = itemType,
            sortBy = sortBy,
            sortOrder = sortOrder,
            limit = limit.toString(),
            fields = fields,
            builderBlock = builderBlock,
        )
        return client.newCall(
            makeApiRequest(userCredentials, url),
        ).execute().parseAs(ItemsListDto.serializer(serializer))
    }

    fun getItemsContainer(
        containerName: String,
        sortBy: String,
        sortOrder: String = "Ascending",
        fields: String? = null,
        limit: Int,
        loadMore: Boolean = false,
        builderBlock: (HttpUrl.Builder.() -> Unit) = {},
    ): MediaItemsContainer {
        val items = getItemsList(
            sortBy = sortBy,
            sortOrder = sortOrder,
            fields = fields,
            limit = limit,
            builderBlock = builderBlock,
        )

        val more = if (loadMore) {
            getMoreItems(
                sortBy = sortBy,
                sortOrder = sortOrder,
                fields = fields,
                limit = limit,
                builderBlock = builderBlock,
            )
        } else {
            null
        }

        return items.toMediaItemsContainer(
            containerName,
            userCredentials.serverUrl,
            more = more,
        )
    }

    fun getContinuousItemList(
        sortBy: String,
        sortOrder: String = "Ascending",
        fields: String? = null,
        limit: Int,
        builderBlock: (HttpUrl.Builder.() -> Unit) = {},
    ): PagedData<MediaItemsContainer> {
        return fetchItems(
            sortBy = sortBy,
            sortOrder = sortOrder,
            fields = fields,
            limit = limit,
            builderBlock = builderBlock,
        ) { item ->
            MediaItemsContainer.Item(item.toMediaItem(userCredentials.serverUrl))
        }
    }

    private fun getMoreItems(
        sortBy: String,
        sortOrder: String,
        fields: String? = null,
        limit: Int,
        builderBlock: HttpUrl.Builder.() -> Unit,
    ): PagedData.Continuous<EchoMediaItem> {
        return fetchItems(
            sortBy = sortBy,
            sortOrder = sortOrder,
            fields = fields,
            limit = limit,
            builderBlock = builderBlock,
        ) { item ->
            item.toMediaItem(userCredentials.serverUrl)
        }
    }

    private fun <R : Any> fetchItems(
        sortBy: String,
        sortOrder: String,
        fields: String? = null,
        limit: Int,
        builderBlock: HttpUrl.Builder.() -> Unit,
        transform: (T) -> R,
    ): PagedData.Continuous<R> {
        return PagedData.Continuous { cont ->
            val start = cont?.toInt() ?: 0
            if (start == -1) return@Continuous Page(emptyList(), null)

            val url = createUrl(
                itemType = itemType,
                sortBy = sortBy,
                sortOrder = sortOrder,
                startIndex = start.toString(),
                limit = limit.toString(),
                fields = fields,
                builderBlock = builderBlock,
            )

            val items = withContext(Dispatchers.IO) {
                client.newCall(
                    makeApiRequest(userCredentials, url),
                ).execute().parseAs(ItemsListDto.serializer(serializer))
            }

            val nextPage = if (start + limit <= items.totalRecordCount) {
                start + limit
            } else {
                -1
            }

            Page(
                data = items.items.map { transform(it) },
                continuation = nextPage.toString(),
            )
        }
    }
}
