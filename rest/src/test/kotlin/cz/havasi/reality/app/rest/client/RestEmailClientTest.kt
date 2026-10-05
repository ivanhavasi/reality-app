package cz.havasi.reality.app.rest.client

import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.EmailNotification
import cz.havasi.reality.app.model.NotificationFilter
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.model.command.SendEmailCommand
import cz.havasi.reality.app.rest.client.api.MailjetApi
import cz.havasi.reality.app.rest.client.model.MailjetEmail
import cz.havasi.reality.app.rest.client.model.MailjetEmailWrapper
import cz.havasi.reality.app.rest.client.model.MailjetEmailsWrapper
import cz.havasi.reality.app.rest.util.APARTMENT
import cz.havasi.reality.app.rest.util.LAND_PLOT
import cz.havasi.reality.app.rest.util.normalizeSpaces
import kotlinx.coroutines.test.runTest
import org.jboss.resteasy.reactive.RestResponse
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.Optional
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

internal class RestEmailClientTest {
    private val sent = mutableListOf<MailjetEmailWrapper>()
    private val client = RestEmailClient(
        object : MailjetApi {
            override suspend fun sendEmails(body: MailjetEmailWrapper): RestResponse<MailjetEmailsWrapper> {
                sent += body
                return RestResponse.ok(MailjetEmailsWrapper(emptyList()))
            }
        },
        Optional.of("user"),
        Optional.of("password"),
    )
    private val notification = EmailNotification(
        id = "n1",
        name = "plots",
        filter = NotificationFilter(BuildingType.LAND, TransactionType.SALE, null, null, listOf("BUILDING_PLOT")),
        userId = "u1",
        updatedAt = OffsetDateTime.MIN,
        createdAt = OffsetDateTime.MIN,
        email = "test@example.com",
        enabled = true,
    )

    private suspend fun emailFor(apartment: cz.havasi.reality.app.model.Apartment): MailjetEmail {
        client.sendEmail(SendEmailCommand(listOf(notification), apartment))
        return sent.last().messages.single()
    }

    @Test
    fun `land email uses labels and units and prints no nulls`() = runTest {
        val email = emailFor(LAND_PLOT)

        assertEquals("Building plot 1 200 m² for sale in Radotín, Praha for 3 200 000 CZK", email.subject.normalizeSpaces())
        listOf(email.subject, email.textPart, email.htmlPart).forEach { part ->
            assertFalse(part.contains("null"), part)
            assertFalse(part.contains("BUILDING_PLOT"), part)
            assertFalse(part.contains("LAND"), part)
            assertFalse(part.contains("Locality("), part)
            assertFalse(part.contains("Apartment"), part)
        }
        assertContains(email.textPart, "Land Listing")
        assertContains(email.textPart, "Location: Radotín, Praha")
        assertContains(email.textPart, "Type: Building plot")
        assertContains(email.textPart.normalizeSpaces(), "Size: 1 200 m²")
        assertContains(email.htmlPart, "<h1>Land Listing</h1>")
        assertContains(email.htmlPart, "<strong>Type:</strong> Building plot")
        assertContains(email.htmlPart.normalizeSpaces(), "<strong>Size:</strong> 1 200 m²")
    }

    @Test
    fun `apartment email keeps the disposition`() = runTest {
        val email = emailFor(APARTMENT)

        assertEquals(
            "Apartment 2+kk 65 m² for sale in Havlíčkova, Žižkov, Praha for 7 000 000 CZK",
            email.subject.normalizeSpaces(),
        )
        assertContains(email.textPart, "Apartment Listing")
        assertContains(email.textPart, "Type: Apartment 2+kk")
        assertFalse(email.textPart.contains("null"))
    }
}
