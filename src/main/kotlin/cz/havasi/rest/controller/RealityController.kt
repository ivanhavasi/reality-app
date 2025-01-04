package cz.havasi.rest.controller

import cz.havasi.model.Apartment
import cz.havasi.model.BuildingType
import cz.havasi.model.CurrencyType
import cz.havasi.model.EmailNotification
import cz.havasi.model.Locality
import cz.havasi.model.Notification
import cz.havasi.model.NotificationFilter
import cz.havasi.model.TransactionType
import cz.havasi.model.WebhookNotification
import cz.havasi.model.event.NotificationEvent
import cz.havasi.rest.client.MailjetClient
import cz.havasi.service.RealityService
import io.quarkus.logging.Log
import jakarta.enterprise.event.Event
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.time.OffsetDateTime

@Path("/hello")
internal class RealityController(
    private val realityService: RealityService,
    private val eventSender: Event<NotificationEvent>,
    @RestClient private val service: MailjetClient,
) {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    suspend fun searchEstates(): String {
        Log.info("Searching estates")
        realityService.fetchAndSaveApartmentsForSale()

        return "Hello world"
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    suspend fun test(): String {
        Log.info("Sending event")
        val apartment = Apartment(
            id = "1",
            mainCategory = BuildingType.APARTMENT,
            transactionType = TransactionType.SALE,
            price = 1000000.0,
            sizeInM2 = 100.0,
            fingerprint = "test",
            name = "Nice apartment by the sea",
            url = "www.sme.sk",
            pricePerM2 = 123000.0,
            currency = CurrencyType.CZK,
            locality = Locality("Brno1", "Brno2", "Brno3", "Brno4", null, null),
            subCategory = "subCategory",
            images = emptyList(),
            description = "Test desc",
        )
        val notification = EmailNotification(
            id = "1",
            name = "Ivans email notif",
            filter = NotificationFilter(
                buildingType = BuildingType.APARTMENT,
                transactionType = TransactionType.SALE,
                price = null,
                size = null,
            ),
            updatedAt = OffsetDateTime.now(),
            createdAt = OffsetDateTime.now(),
            email = "ivohavasi@gmail.com",
        )
        val notification2 = WebhookNotification(
            id = "1",
            name = "Ivans email notif",
            filter = NotificationFilter(
                buildingType = BuildingType.APARTMENT,
                transactionType = TransactionType.SALE,
                price = null,
                size = null,
            ),
            updatedAt = OffsetDateTime.now(),
            createdAt = OffsetDateTime.now(),
            url = "testUrl",
        )
        (0..1).forEach {
            Log.info("Sending event $it")
            eventSender.fire(NotificationEvent(listOf(notification,notification2), apartment))
        }
        println("AFTEREER")

        return "Hello world"
    }
}
