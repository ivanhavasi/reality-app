package cz.havasi.reality.app.rest.client

import cz.havasi.reality.app.model.Apartment
import cz.havasi.reality.app.model.command.SendApartmentDiscordMessageCommand
import cz.havasi.reality.app.model.type.ProviderType
import cz.havasi.reality.app.rest.client.api.DiscordApi
import cz.havasi.reality.app.rest.client.model.DiscordEmbed
import cz.havasi.reality.app.rest.client.model.DiscordField
import cz.havasi.reality.app.rest.client.model.DiscordUrl
import cz.havasi.reality.app.rest.client.model.DiscordWebhookBody
import cz.havasi.reality.app.service.util.firstCapitalOthersLowerCase
import cz.havasi.reality.app.rest.util.formatToNumberWithSpaces
import cz.havasi.reality.app.service.client.DiscordClient
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.jboss.resteasy.reactive.RestResponse

@ApplicationScoped
internal class RestDiscordClient(
    @RestClient private val discordApi: DiscordApi,
) : DiscordClient {
    override suspend fun sendApartmentDiscordMessage(command: SendApartmentDiscordMessageCommand): String =
        discordApi.sendWebhook(
            webhookId = command.webhookId,
            webhookToken = command.webhookToken,
            body = command.apartment.toBody(),
        )
            .handleResponse()

    private fun RestResponse<String>.handleResponse(): String {
        if (status >= 400) {
            Log.error("Could not send apartment to discord. Status: $status, $entity")
            throw IllegalStateException("Could not send apartment to discord. Status: $status, $entity")
        }
        return entity ?: "Empty response from Discord webhook"
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
            ProviderType.SREALITY -> 13705505 // red
            ProviderType.IDNES -> 2455546 // blue
            ProviderType.BEZREALITKY -> 3773963 // green
            ProviderType.UNKNOWN -> 14593814 // yellow
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
