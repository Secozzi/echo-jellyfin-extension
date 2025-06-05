package dev.brahmkshatriya.echo.extension.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginDto(
    val accessToken: String,
    val user: UserDto,
) {
    @Serializable
    class UserDto(
        val id: String,
        val name: String,
    )
}
