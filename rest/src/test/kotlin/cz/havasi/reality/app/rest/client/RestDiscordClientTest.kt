package cz.havasi.reality.app.rest.client

import cz.havasi.reality.app.model.Apartment
import cz.havasi.reality.app.model.command.SendApartmentDiscordMessageCommand
import cz.havasi.reality.app.rest.client.api.DiscordApi
import cz.havasi.reality.app.rest.client.model.DiscordWebhookBody
import cz.havasi.reality.app.rest.util.HOUSE
import cz.havasi.reality.app.rest.util.LAND_PLOT
import cz.havasi.reality.app.rest.util.duplicate
import cz.havasi.reality.app.rest.util.normalizeSpaces
import kotlinx.coroutines.test.runTest
import org.jboss.resteasy.reactive.RestResponse
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

internal class RestDiscordClientTest {
    private val sent = mutableListOf<DiscordWebhookBody>()
    private val client = RestDiscordClient(
        object : DiscordApi {
            override suspend fun sendWebhook(webhookId: String, webhookToken: String, body: DiscordWebhookBody): RestResponse<String> {
                sent += body
                return RestResponse.ok("")
            }
        },
    )

    private suspend fun fieldsFor(apartment: Apartment): Map<String, String> {
        client.sendApartmentDiscordMessage(SendApartmentDiscordMessageCommand("id", "token", apartment))
        return sent.last().embeds.single().fields.associate { it.name to it.value.normalizeSpaces() }
    }

    @Test
    fun `land message uses labels and units and prints no nulls`() = runTest {
        val fields = fieldsFor(LAND_PLOT)

        assertEquals("1 200 m²", fields["Size"])
        assertEquals("Radotín, Praha", fields["Location"])
        assertEquals("Building plot", fields["Type"])
        assertEquals("3 200 000", fields["Price"])
        fields.values.forEach { assertFalse(it.contains("null"), it) }
    }

    @Test
    fun `house message has a plot field after the size`() = runTest {
        client.sendApartmentDiscordMessage(SendApartmentDiscordMessageCommand("id", "token", HOUSE))
        val fields = sent.last().embeds.single().fields

        assertEquals(listOf("Price", "Size", "Plot"), fields.take(3).map { it.name })
        assertEquals("156 m²", fields[1].value.normalizeSpaces())
        assertEquals("1 296 m²", fields[2].value.normalizeSpaces())
        assertEquals("Family house", fields.single { it.name == "Type" }.value)
    }

    @Test
    fun `land and houses without a known plot have no plot field`() = runTest {
        assertNull(fieldsFor(LAND_PLOT)["Plot"])
        assertNull(fieldsFor(HOUSE.copy(landSizeInM2 = null))["Plot"])
    }

    @Test
    fun `price on request duplicate is not shown as a discount`() = runTest {
        val fields = fieldsFor(LAND_PLOT.copy(duplicates = listOf(duplicate(0.0))))

        assertNull(fields["Discount Price"])
    }

    @Test
    fun `cheaper duplicate is shown as a discount`() = runTest {
        val fields = fieldsFor(LAND_PLOT.copy(duplicates = listOf(duplicate(0.0), duplicate(3_000_000.0))))

        assertEquals("3 000 000", fields["Discount Price"])
    }
}
