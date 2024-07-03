package dev.brahmkshatriya.echo.extension.models

import kotlinx.serialization.Serializable

@Serializable
class LoginDto(
    val accessToken: String,
    val user: UserDto,
) {
    @Serializable
    class UserDto(
        val id: String,
        val name: String,
    )
}
