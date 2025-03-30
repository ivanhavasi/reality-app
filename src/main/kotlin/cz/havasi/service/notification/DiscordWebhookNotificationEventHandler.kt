package cz.havasi.service.notification

import cz.havasi.model.Apartment
import cz.havasi.model.DiscordWebhookNotification
import cz.havasi.model.enum.ProviderType
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
import jakarta.enterprise.event.Observes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.eclipse.microprofile.rest.client.inject.RestClient

@ApplicationScoped
internal class DiscordWebhookNotificationEventHandler(
    @RestClient private val discordClient: DiscordClient,
) : NotificationEventHandler<DiscordWebhookNotification> {

    override fun handleNotifications(@Observes event: HandleNotificationsEvent<DiscordWebhookNotification>) {
        Log.info("Handling ${event.notifications.size} discord notifications for apartment ${event.apartment.id}")
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
                    color = resolveColor(),
                    thumbnail = images.getThumbnailData(),
                    fields = getFieldsData(),
                ),
            ),
        )

    private fun Apartment.resolveColor(): Int =
        when (provider) {
            ProviderType.SREALITY -> 13705505
            ProviderType.IDNES -> 2455546
            ProviderType.BEZREALITKY -> 3773963
            ProviderType.UNKNOWN -> 14593814
        }

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
    ).applyDiscountFieldIfNeeded(this)

    private fun List<DiscordField>.applyDiscountFieldIfNeeded(apartment: Apartment): List<DiscordField> {
        val discountPrice = apartment.calculateDiscountPrice()

        return if (discountPrice < apartment.price) {
            val list = toMutableList()
            list.add(
                1,
                DiscordField(
                    name = "Discount Price",
                    value = discountPrice.formatToNumberWithSpaces(),
                    inline = true,
                ),
            )
            list
        } else {
            this
        }
    }

    private fun Apartment.calculateDiscountPrice(): Double {
        val minPrice = duplicates.minByOrNull { it.price }
        return if (minPrice != null && minPrice.price < price) {
            minPrice.price
        } else {
            price
        }
    }
}
