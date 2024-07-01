package dev.brahmkshatriya.echo.extension.models

import kotlinx.serialization.Serializable

@Serializable
class LoginDto(
    val user: UserDto,
    val serverId: String,
    val accessToken: String,
) {
    @Serializable
    class UserDto(
        val name: String,
        val id: String,
        val primaryImageTag: String? = null,
    )
}
