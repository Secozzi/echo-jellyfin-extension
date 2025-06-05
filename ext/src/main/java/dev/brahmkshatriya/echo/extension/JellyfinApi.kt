package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.helpers.ClientException
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.extension.dto.LoginDto
import extension.ext.BuildConfig
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.put
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

class JellyfinApi {
    private val json = Json {
        ignoreUnknownKeys = true
        namingStrategy = PascalCaseToCamelCase
    }

    private var userCredentials = UserCredentials.EMPTY

    private val client = OkHttpClient().newBuilder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Accept", "application/json, application/octet-stream;q=0.9, */*;q=0.8")
                .build()

            if (request.url.encodedPath.endsWith("AuthenticateByName")) {
                return@addInterceptor chain.proceed(request)
            }

            if (userCredentials.accessToken.isEmpty()) {
                return@addInterceptor Response.Builder().apply {
                    request(request)
                    protocol(Protocol.HTTP_1_1)
                    code(401)
                    message("Unauthorized")
                }.build()
            }

            val authRequest = request.newBuilder()
                .addHeader("X-MediaBrowser-Token", userCredentials.accessToken)
                .build()

            chain.proceed(authRequest)
        }
        .build()

    // ================ Login =================

    suspend fun onLogin(data: Map<String, String?>, deviceId: String): List<User> {
        val serverUrl = data["address"]!!

        val body = buildJsonObject {
            put("Username", data["username"]!!)
            put("Pw", data["password"]!!)
        }.toRequestBody()

        val headers = Headers.headersOf(
            "X-Emby-Authorization",
            """MediaBrowser Client="${BuildConfig.APP_NAME}", Device="${BuildConfig.DEVICE_NAME}", DeviceId="$deviceId", Version="${BuildConfig.APP_VER}"""",
        )

        val url = serverUrl.toHttpUrl().newBuilder().apply {
            addPathSegment("Users")
            addPathSegment("AuthenticateByName")
        }.build()

        val loginData = client.post(url, headers, body).parseAs<LoginDto>()

        val user = User(
            id = loginData.user.id,
            name = loginData.user.name,
            cover = "$serverUrl/Users/${loginData.user.id}/Images/Primary".toImageHolder(),
            extras = mapOf(
                "accessToken" to loginData.accessToken,
                "serverUrl" to serverUrl,
            ),
        )

        return listOf(user)
    }

    fun setUser(user: User?) {
        userCredentials = user?.let {
            UserCredentials(
                userId = it.id,
                userName = it.name,
                accessToken = it.extras["accessToken"]!!,
                serverUrl = it.extras["serverUrl"]!!,
            )
        } ?: UserCredentials.EMPTY
    }

    fun getUser(): User? {
        if (userCredentials.accessToken.isEmpty()) return null

        return User(
            id = userCredentials.userId,
            name = userCredentials.userName,
            cover = "${userCredentials.serverUrl}/Users/${userCredentials.userId}/Images/Primary".toImageHolder(),
        )
    }

    // ================ Utils =================

    fun checkAuth() {
        if (userCredentials.accessToken.isEmpty()) {
            throw ClientException.LoginRequired()
        }
    }

    private inline fun <reified T> Response.parseAs(): T {
        return json.decodeFromStream(body.byteStream())
    }

    private inline fun <reified T> T.toRequestBody(): RequestBody {
        return json.encodeToString(this).toRequestBody(
            "application/json".toMediaType(),
        )
    }
}

data class UserCredentials(
    val userId: String,
    val userName: String,
    val accessToken: String,
    val serverUrl: String,
) {
    companion object {
        val EMPTY = UserCredentials("", "", "", "")
    }
}
