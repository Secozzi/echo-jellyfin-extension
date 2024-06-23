package dev.brahmkshatriya.echo.extension.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class LoginDto(
    @SerialName("User") val user: UserDto,
    @SerialName("ServerId") val serverId: String,
    @SerialName("AccessToken") val accessToken: String,
) {
    @Serializable
    class UserDto(
        @SerialName("Name") val name: String,
        @SerialName("Id") val id: String,
        @SerialName("PrimaryImageTag") val pImgtag: String? = null,
    )
}
