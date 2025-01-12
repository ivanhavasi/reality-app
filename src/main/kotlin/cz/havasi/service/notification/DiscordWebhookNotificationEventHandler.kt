package cz.havasi.service.notification

import cz.havasi.model.Apartment
import cz.havasi.model.DiscordWebhookNotification
import cz.havasi.model.event.HandleNotificationsEvent
import cz.havasi.rest.client.DiscordClient
import cz.havasi.rest.client.model.DiscordEmbed
import cz.havasi.rest.client.model.DiscordField
import cz.havasi.rest.client.model.DiscordUrl
import cz.havasi.rest.client.model.DiscordWebhookBody
import cz.havasi.service.util.firstCapitalOthersLowerCase
import cz.havasi.service.util.formatToNumberWithSpaces
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.eclipse.microprofile.rest.client.inject.RestClient

@ApplicationScoped
internal class DiscordWebhookNotificationEventHandler(
    @RestClient private val discordClient: DiscordClient,
) : NotificationEventHandler<DiscordWebhookNotification> {
    override fun handleNotifications(event: HandleNotificationsEvent<DiscordWebhookNotification>) {
        Log.debug("Handling ${event.notifications.size} email notifications for apartment ${event.apartment.id}")
        CoroutineScope(Dispatchers.IO).launch {
            event.sendWebhooks()
        }
    }

    private suspend fun HandleNotificationsEvent<DiscordWebhookNotification>.sendWebhooks() {
        notifications.forEach { notification ->
            try {
                val response = discordClient.sendWebhook(
                    webhookId = notification.webhookId,
                    webhookToken = notification.token,
                    body = apartment.toBody(),
                )
                Log.info("Webhooks response status: ${response.status} for ${notifications.size} webhooks")
            } catch (e: Exception) {
                Log.error("Error sending webhooks", e)
                throw e
            }
        }
    }

    private fun Apartment.toBody(): DiscordWebhookBody =
        DiscordWebhookBody(
            embeds = listOf(
                DiscordEmbed(
                    title = name,
                    description = description,
                    url = url,
                    color = 65280,
                    thumbnail = images.getThumbnailData(),
                    fields = getFieldsData(),
                ),
            ),
        )

    private fun List<String>.getThumbnailData() = DiscordUrl(
        url = getOrNull(0) ?: "https://picsum.photos/500/400",
    )

    private fun Apartment.getFieldsData() = listOf(
        DiscordField(
            name = "Price",
            value = price.formatToNumberWithSpaces(),
            inline = true,
        ),
        DiscordField(
            name = "Size",
            value = sizeInM2.toInt().toString(),
            inline = true,
        ),
        DiscordField(
            name = "Price per m²",
            value = pricePerM2?.formatToNumberWithSpaces() ?: "Unknown",
            inline = true,
        ),
        DiscordField(
            name = "Location",
            value = "${locality.street}, ${locality.city}, ${locality.district}",
            inline = false,
        ),
        DiscordField(
            name = "Type",
            value = "${mainCategory.name} - $subCategory",
            inline = true,
        ),
        DiscordField(
            name = "Transaction Type",
            value = transactionType.name.firstCapitalOthersLowerCase(),
            inline = true,
        ),
    )
}
