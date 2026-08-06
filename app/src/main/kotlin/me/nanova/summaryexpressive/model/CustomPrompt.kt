package me.nanova.summaryexpressive.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class CustomPrompt(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String
)
