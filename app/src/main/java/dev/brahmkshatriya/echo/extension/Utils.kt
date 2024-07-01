package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import okhttp3.CacheControl
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.TimeUnit.MINUTES

@Suppress("MagicNumber")
private val DEFAULT_CACHE_CONTROL = CacheControl.Builder().maxAge(10, MINUTES).build()
private val DEFAULT_HEADERS = Headers.Builder().build()
private val DEFAULT_BODY: RequestBody = FormBody.Builder().build()

@Suppress("FunctionNaming")
fun GET(
    url: HttpUrl,
    headers: Headers = DEFAULT_HEADERS,
    cache: CacheControl = DEFAULT_CACHE_CONTROL,
): Request {
    return Request.Builder()
        .url(url)
        .headers(headers)
        .cacheControl(cache)
        .build()
}

@Suppress("FunctionNaming")
fun POST(
    url: HttpUrl,
    headers: Headers = DEFAULT_HEADERS,
    body: RequestBody = DEFAULT_BODY,
    cache: CacheControl = DEFAULT_CACHE_CONTROL,
): Request {
    return Request.Builder()
        .url(url)
        .post(body)
        .headers(headers)
        .cacheControl(cache)
        .build()
}

@Suppress("FunctionNaming")
fun DELETE(
    url: HttpUrl,
    headers: Headers = DEFAULT_HEADERS,
    body: RequestBody = DEFAULT_BODY,
    cache: CacheControl = DEFAULT_CACHE_CONTROL,
): Request {
    return Request.Builder()
        .url(url)
        .delete(body)
        .headers(headers)
        .cacheControl(cache)
        .build()
}

fun createItemsUrl(
    userCredentials: UserCredentials,
    itemType: String,
    sortBy: String,
    fields: String? = null,
    limit: String = "15",
    sortOrder: String = "Ascending",
    startIndex: String = "0",
    builderBlock: (HttpUrl.Builder.() -> Unit) = {},
): HttpUrl {
    val serverUrl = userCredentials.serverUrl.toHttpUrl()

    return serverUrl.newBuilder().apply {
        addPathSegment("Users")
        addPathSegment(userCredentials.userId)
        addPathSegment("Items")
        fields?.let {
            addEncodedQueryParameter("Fields", it)
        }
        addEncodedQueryParameter("IncludeItemTypes", itemType)
        addQueryParameter("Limit", limit)
        addQueryParameter("Recursive", "true")
        addEncodedQueryParameter("SortBy", sortBy)
        addQueryParameter("SortOrder", sortOrder)
        addQueryParameter("StartIndex", startIndex)
        apply(builderBlock)
    }.build()
}

fun getHeaders(userCredentials: UserCredentials): Headers {
    return Headers.Builder().apply {
        add("Accept", "application/json, application/octet-stream;q=0.9, */*;q=0.8")
        add("X-MediaBrowser-Token", userCredentials.accessToken)
    }.build()
}

fun makeApiRequest(userCredentials: UserCredentials, url: HttpUrl): Request {
    val headers = getHeaders(userCredentials)
    return GET(url, headers = headers)
}

fun makeApiRequest(userCredentials: UserCredentials, url: HttpUrl, requestBody: RequestBody): Request {
    val headers = getHeaders(userCredentials)
    return POST(url, headers = headers, body = requestBody)
}

fun String?.toImage(serverUrl: String, id: String): ImageHolder? {
    return this?.let { "$serverUrl/Items/$id/Images/Primary".toImageHolder() }
}

object PascalCaseToCamelCase : JsonNamingStrategy {
    override fun serialNameForJson(
        descriptor: SerialDescriptor,
        elementIndex: Int,
        serialName: String,
    ): String {
        return serialName.replaceFirstChar { it.uppercase() }
    }
}

val json = Json {
    ignoreUnknownKeys = true
    namingStrategy = PascalCaseToCamelCase
}

inline fun <reified T> Response.parseAs(): T {
    return json.decodeFromString(body.string())
}

inline fun <reified T> T.toRequestBody(): RequestBody {
    return json.encodeToString(this).toRequestBody(
        "application/json".toMediaType(),
    )
}

data class UserCredentials(
    val userId: String,
    val accessToken: String,
    val serverId: String,
    val serverUrl: String,
) {
    fun urlBuilder(): HttpUrl.Builder = serverUrl.toHttpUrl().newBuilder()
}

const val TicksPerMs = 10_000
