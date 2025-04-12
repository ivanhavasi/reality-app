package cz.havasi.service.notification

import cz.havasi.model.Apartment
import cz.havasi.model.EmailNotification
import cz.havasi.model.command.SaveSentNotificationsCommand
import cz.havasi.model.event.HandleNotificationsEvent
import cz.havasi.repository.SentNotificationRepository
import cz.havasi.rest.client.MailjetClient
import cz.havasi.rest.client.model.EmailAddress
import cz.havasi.rest.client.model.MailjetEmail
import cz.havasi.rest.client.model.MailjetEmailWrapper
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
internal class EmailNotificationEventHandler(
    @RestClient private val mailjetClient: MailjetClient, // todo, they have limits on max number of messages https://dev.mailjet.com/email/reference/send-emails#v3_1_post_send
    private val sentNotificationRepository: SentNotificationRepository,
) : NotificationEventHandler<EmailNotification> {

    override fun handleNotifications(@Observes event: HandleNotificationsEvent<EmailNotification>) {
        Log.debug("Handling ${event.notifications.size} email notifications for apartment ${event.apartment.id}")
        CoroutineScope(Dispatchers.IO).launch {
            event.sendEmails()
        }
    }

    private suspend fun HandleNotificationsEvent<EmailNotification>.sendEmails(): Unit {
        val emails = notifications.mapToEmail(apartment)
        Log.info("Sending ${emails.size} emails")

        try {
            val response = mailjetClient.sendEmails(MailjetEmailWrapper(messages = emails))
            sentNotificationRepository.saveSentNotifications(
                SaveSentNotificationsCommand(
                    notifications = notifications,
                    apartment = apartment,
                ),
            )

            Log.info("Emails response status: ${response.status} for ${emails.size} emails")
        } catch (e: Exception) { // todo improve error handling, they have special error objects
            Log.error("Error sending emails", e)
            throw e
        }
    }

    private fun List<EmailNotification>.mapToEmail(apartment: Apartment) = map { notification ->
        MailjetEmail(
            from = EMAIL_FROM,
            to = notification.toEmailTo(),
            subject = apartment.toSubject(),
            textPart = apartment.toTextPart(),
            htmlPart = apartment.toHtmlPart(),
        )
    }

    private fun EmailNotification.toEmailTo() =
        listOf(EmailAddress(email = email, name = email))

    private fun Apartment.toSubject() =
        "${mainCategory.name.firstCapitalOthersLowerCase()} for ${transactionType.name.lowercase()} in ${locality.city}, ${locality.street} for ${price.formatToNumberWithSpaces()} CZK"

    private fun Apartment.toTextPart() =
        """
            Apartment Listing

            Name: $name
            Price: ${price.formatToNumberWithSpaces()} $currency
            Size: ${sizeInM2.formatToNumberWithSpaces()} m²
            Price per m²: ${pricePerM2?.formatToNumberWithSpaces() ?: "Unknown"} $currency
            Location: $locality.street, $locality.city, $locality.district
            Type: $mainCategory - $subCategory
            For ${transactionType.name.lowercase()}
            
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
                    <a href="$url">
                        <img src="${images.getMainImage()}" alt="Apartment Image" class="apartment-image">
                    </a>
                    <h2>$name</h2>
                    <div class="details">
                        <p><strong>Price:</strong> ${price.formatToNumberWithSpaces()} $currency</p>
                        <p><strong>Size:</strong> ${sizeInM2.toInt()} m²</p>
                        <p><strong>Price per m²:</strong> ${pricePerM2?.formatToNumberWithSpaces() ?: "Unknown"} $currency</p>
                        <p><strong>Location:</strong> ${locality.street}, ${locality.city}, ${locality.district}</p>
                        <p><strong>Type:</strong> ${mainCategory.name.firstCapitalOthersLowerCase()} - $subCategory</p>
                        <p><strong>For ${transactionType.name.firstCapitalOthersLowerCase()}</strong></p>
                    </div>
                    <p>$description</p>
                    <a href="$url" class="cta-button">View More Details</a>
                </div>
                <div class="footer">
                    <p>&copy; 2024 UnReal Estate Agency. All rights reserved.</p>
                </div>
            </div>
        </body>
        </html>
        """.trimMargin()

    private fun List<String>.getMainImage() =
        getOrNull(0)
            ?: "https://picsum.photos/500/400"

    companion object {
        const val EMAIL_NAME_FROM = "Havasi Reality Watchers"
        const val EMAIL_ADDRESS_FROM = "reality@havasi.me"
        val EMAIL_FROM = EmailAddress(email = EMAIL_ADDRESS_FROM, name = EMAIL_NAME_FROM)
    }
}
