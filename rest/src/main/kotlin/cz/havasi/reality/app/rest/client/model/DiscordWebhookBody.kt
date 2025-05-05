package cz.havasi.reality.app.rest.client.model

import io.quarkus.runtime.annotations.RegisterForReflection

@RegisterForReflection
internal data class DiscordWebhookBody(
    val embeds: List<DiscordEmbed>,
)

@RegisterForReflection
internal data class DiscordEmbed(
    val title: String,
    val description: String?,
    val url: String?,
    val color: Int = 0,
    val thumbnail: DiscordUrl,
    val fields: List<DiscordField>,
)

@RegisterForReflection
internal data class DiscordField(
    val name: String,
    val value: String,
    val inline: Boolean,
)

@RegisterForReflection
internal data class DiscordUrl(
    val url: String,
)
