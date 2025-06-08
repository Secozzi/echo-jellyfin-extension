package dev.brahmkshatriya.echo.extension.dto

import kotlinx.serialization.Serializable

@Serializable
data class LyricListDto(
    val lyrics: List<LyricDto>? = null,
)

@Serializable
data class LyricDto(
    val text: String,
    val start: Long? = null,
)
