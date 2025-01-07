package cz.havasi.rest.controller

import cz.havasi.rest.client.MailjetClient
import cz.havasi.service.RealityService
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.rest.client.inject.RestClient

@Path("/hello") // todo for testing purposes only
internal class RealityController(
    private val realityService: RealityService,
    @RestClient private val client: MailjetClient,
) {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    suspend fun searchEstates(): String {
//        Log.info("Searching estates")
//        realityService.fetchAndSaveApartmentsForSale()
//
//        return "Hello world"

//        val result = client.sendEmails(
//            MailjetEmailWrapper(
//                messages = listOf(
//                    MailjetEmail(
//                        from = EmailAddress(
//                            email = "reality@havasi.me",
//                            name = "Ivan Havasi",
//                        ),
//                        to = listOf(EmailAddress(
//                            email = "ivohavasi@gmail.com",
//                            name = "Ivan Havasi",
//                        )),
//                        subject = "Test Subject",
//                        textPart = "Test text",
//                        htmlPart = "<h1>Test html</h1>",
//                    )
//                ),
//                sandboxMode = false,
//            )
//        )
//        println(result)
//        println("AGTER")
        return "Hello world"
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    suspend fun test(): String {
//        Log.info("Sending event")
//        val apartment = Apartment(
//            id = "1",
//            mainCategory = BuildingType.APARTMENT,
//            transactionType = TransactionType.SALE,
//            price = 1000000.0,
//            sizeInM2 = 100.0,
//            fingerprint = "test",
//            name = "Nice apartment by the sea",
//            url = "www.sme.sk",
//            pricePerM2 = 123000.0,
//            currency = CurrencyType.CZK,
//            locality = Locality("Brno1", "Brno2", "Brno3", "Brno4", null, null),
//            subCategory = "subCategory",
//            images = emptyList(),
//            description = "Test desc",
//        )
//        val notification = EmailNotification(
//            id = "1",
//            name = "Ivans email notif",
//            filter = NotificationFilter(
//                buildingType = BuildingType.APARTMENT,
//                transactionType = TransactionType.SALE,
//                price = null,
//                size = null,
//            ),
//            updatedAt = OffsetDateTime.now(),
//            createdAt = OffsetDateTime.now(),
//            email = "ivohavasi@gmail.com",
//        )
//        val notification2 = WebhookNotification(
//            id = "1",
//            name = "Ivans email notif",
//            filter = NotificationFilter(
//                buildingType = BuildingType.APARTMENT,
//                transactionType = TransactionType.SALE,
//                price = null,
//                size = null,
//            ),
//            updatedAt = OffsetDateTime.now(),
//            createdAt = OffsetDateTime.now(),
//            url = "testUrl",
//        )
//        (0..0).forEach {
//            Log.info("Sending event $it")
////            eventSender3.fire(HandleNotificationsEvent(apartment, listOf(notification)))
//        }
//        println("AFTEREER")

        return "Hello world"
    }
}
