package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.helpers.ContinuationCallback.Companion.await
import dev.brahmkshatriya.echo.common.models.Date
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.JsonNamingStrategy
import okhttp3.CacheControl
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.util.concurrent.TimeUnit.MINUTES

object PascalCaseToCamelCase : JsonNamingStrategy {
    override fun serialNameForJson(
        descriptor: SerialDescriptor,
        elementIndex: Int,
        serialName: String,
    ): String {
        return serialName.replaceFirstChar { it.uppercase() }
    }
}

fun randomString(length: Int = 16): String {
    val charPool = ('a'..'z') + ('0'..'9')

    return buildString(length) {
        for (i in 0 until length) {
            append(charPool.random())
        }
    }
}

fun String?.getImageUrl(serverUrl: String, id: String, name: String = "Primary"): ImageHolder? {
    return this?.let { tag ->
        serverUrl.toHttpUrl().newBuilder().apply {
            addPathSegment("Items")
            addPathSegment(id)
            addPathSegment("Images")
            addPathSegment(name)
            addQueryParameter("tag", tag)
        }.build().toString()
    }?.toImageHolder()
}

fun String.toDate(): Date {
    return this.substringBefore("T").split("-").let { (year, month, day) ->
        Date(year.toInt(), month.toIntOrNull(), day.toIntOrNull())
    }
}

private val DEFAULT_CACHE_CONTROL = CacheControl.Builder().maxAge(10, MINUTES).build()
private val DEFAULT_HEADERS = Headers.Builder().build()
private val DEFAULT_BODY: RequestBody = FormBody.Builder().build()

suspend fun OkHttpClient.get(
    url: HttpUrl,
    headers: Headers = DEFAULT_HEADERS,
    cache: CacheControl = DEFAULT_CACHE_CONTROL,
): Response {
    return newCall(
        Request.Builder()
            .url(url)
            .headers(headers)
            .cacheControl(cache)
            .build(),
    ).await()
}

suspend fun OkHttpClient.post(
    url: HttpUrl,
    headers: Headers = DEFAULT_HEADERS,
    body: RequestBody = DEFAULT_BODY,
    cache: CacheControl = DEFAULT_CACHE_CONTROL,
): Response {
    return newCall(
        Request.Builder()
            .url(url)
            .post(body)
            .headers(headers)
            .cacheControl(cache)
            .build(),
    ).await()
}

suspend fun OkHttpClient.delete(
    url: HttpUrl,
    headers: Headers = DEFAULT_HEADERS,
    body: RequestBody = DEFAULT_BODY,
    cache: CacheControl = DEFAULT_CACHE_CONTROL,
): Response {
    return newCall(
        Request.Builder()
            .url(url)
            .delete(body)
            .headers(headers)
            .cacheControl(cache)
            .build(),
    ).await()
}

const val TICKS_PER_MS = 10_000
