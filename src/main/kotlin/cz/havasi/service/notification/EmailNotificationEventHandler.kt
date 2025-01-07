package cz.havasi.service.notification

import cz.havasi.model.Apartment
import cz.havasi.model.EmailNotification
import cz.havasi.model.event.HandleNotificationsEvent
import cz.havasi.rest.client.MailjetClient
import cz.havasi.rest.client.model.EmailAddress
import cz.havasi.rest.client.model.MailjetEmail
import cz.havasi.rest.client.model.MailjetEmailWrapper
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.eclipse.microprofile.rest.client.inject.RestClient

@ApplicationScoped
internal class EmailNotificationEventHandler(
    // todo, shouldnt this be just catching ApartmentCreatedEvent and then finding its own notifications? Better event handling
    @RestClient private val mailjetClient: MailjetClient, // todo, they have limits on max number of messages https://dev.mailjet.com/email/reference/send-emails#v3_1_post_send
) : NotificationEventHandler<EmailNotification> {

    override fun handleNotifications(@Observes event: HandleNotificationsEvent<EmailNotification>) {
        Log.info("Handling ${event.notifications.size} email notifications for apartment ${event.apartment.id}")
        CoroutineScope(Dispatchers.IO).launch {
            event.sendEmails()
        }
    }

    private suspend fun HandleNotificationsEvent<EmailNotification>.sendEmails(): Unit {
        val emails = notifications.map { notification ->
            MailjetEmail(
                from = EMAIL_FROM,
                to = notification.toEmailTo(),
                subject = apartment.toSubject(),
                textPart = apartment.toTextPart(),
                htmlPart = apartment.toHtmlPart(),
            )
        }

        Log.info("Sending ${emails.size} emails")
        try {
            val response = mailjetClient.sendEmails(MailjetEmailWrapper(messages = emails))
            Log.info("Emails response status: ${response.status} for ${emails.size} emails")
        } catch (e: Exception) { // todo improve error handling, they have special error objects
            Log.error("Error sending emails", e)
            throw e
        }
    }

    private fun EmailNotification.toEmailTo() =
        listOf(EmailAddress(email = email, name = email))

    private fun Apartment.toSubject() =
        "$mainCategory for $transactionType in ${locality.city}, ${locality.street} for $price CZK" // todo maybe make the enums into lower case?

    private fun Apartment.toTextPart() =
        """
            Apartment Listing

            Name: $name
            Price: $price $currency
            Size: $sizeInM2 m²
            Price per m²: $pricePerM2 $currency
            Location: $locality.street, $locality.city, $locality.district
            Type: $mainCategory - $subCategory
            Transaction: $transactionType
            
            Description:
            $description
            
            View more details: $url
        """.trimMargin()

    private fun Apartment.toHtmlPart() =
        """
        <!DOCTYPE html>
        <html lang="en">
        <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Apartment Details</title>
        <style>
            body {
                font-family: Arial, sans-serif;
                margin: 0;
                padding: 0;
                background-color: #f9fafc;
            }
            .container {
                max-width: 600px;
                margin: 20px auto;
                border: 1px solid #ddd;
                border-radius: 8px;
                overflow: hidden;
                background-color: #fff;
            }
            .header {
                background-color: #4CAF50;
                color: white;
                text-align: center;
                padding: 20px;
            }
            .content {
                padding: 20px;
            }
            .apartment-image {
                width: 100%;
                height: auto;
                border-radius: 8px;
            }
            .details {
                margin-top: 20px;
            }
            .details p {
                margin: 5px 0;
            }
            .footer {
                text-align: center;
                font-size: 12px;
                color: #888;
                margin: 20px 0;
            }
            .cta-button {
                display: inline-block;
                margin-top: 20px;
                padding: 10px 20px;
                background-color: #4CAF50;
                color: white;
                text-decoration: none;
                border-radius: 5px;
            }
        </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>Apartment Listing</h1>
                </div>
                <div class="content">
                    <img src="${images.getOrNull(0) ?: "https://picsum.photos/500/400"}" alt="Apartment Image" class="apartment-image">
                    <h2>$name</h2>
                    <div class="details">
                        <p><strong>Price:</strong> $price $currency</p>
                        <p><strong>Size:</strong> $sizeInM2 m²</p>
                        <p><strong>Price per m²:</strong> $pricePerM2 $currency</p>
                        <p><strong>Location:</strong> ${locality.street}, ${locality.city}, ${locality.district}</p>
                        <p><strong>Type:</strong> $mainCategory - $subCategory</p>
                        <p><strong>Transaction:</strong> $transactionType</p>
                    </div>
                    <p>$description</p>
                    <a href="$url" class="cta-button">View More Details</a>
                </div>
                <div class="footer">
                    <p>&copy; 2024 Real Estate Agency. All rights reserved.</p>
                </div>
            </div>
        </body>
        </html>
        """.trimMargin()

    companion object {
        const val EMAIL_NAME_FROM = "Havasi Reality Watchers"
        const val EMAIL_ADDRESS_FROM = "reality@havasi.me"
        val EMAIL_FROM = EmailAddress(email = EMAIL_ADDRESS_FROM, name = EMAIL_NAME_FROM)
    }
}
