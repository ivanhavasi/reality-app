package cz.havasi.service.notification

import cz.havasi.model.Apartment
import cz.havasi.model.EmailNotification
import cz.havasi.model.command.HandleNotificationsCommand
import cz.havasi.rest.client.MailjetClient
import cz.havasi.rest.client.model.EmailAddress
import cz.havasi.rest.client.model.MailjetEmail
import cz.havasi.rest.client.model.MailjetEmailWrapper
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.eclipse.microprofile.rest.client.inject.RestClient

@ApplicationScoped
internal class EmailNotificationHandler(
    @RestClient private val mailjetClient: MailjetClient,
) : NotificationHandler<EmailNotification> {

    override fun handleNotifications(command: HandleNotificationsCommand<EmailNotification>) {
        Log.info("Handling ${command.notifications.size} email notifications for apartment ${command.apartment.id}")
        CoroutineScope(Dispatchers.IO).launch {
            delay(1000) // todo remove
            Log.info("After wait")
            command.sendEmails()
        }
    }

    private suspend fun HandleNotificationsCommand<EmailNotification>.sendEmails(): Unit {
        val emails = notifications.map { notification ->
            MailjetEmail(
                from = EMAIL_FROM,
                to = notification.toEmailTo(),
                subject = apartment.toSubject(),
                textPart = apartment.toTextPart(),
                htmlPart = apartment.toTextPart(), // todo add html
            )
        }

        Log.info("Sending ${emails.size} emails")
        val response = mailjetClient.sendEmails(MailjetEmailWrapper(messages = emails, sandboxMode = true))
        Log.info("Emails response status: ${response.status} for ${emails.size} emails")
    }

    private fun EmailNotification.toEmailTo() =
        listOf(EmailAddress(email = email, name = email))

    private fun Apartment.toSubject() =
        "$mainCategory for $transactionType in ${locality.city} for $price CZK" // todo maybe make the enums into lower case?

    private fun Apartment.toTextPart() = // todo create better text
        """
            |Apartment for $transactionType
            |$name
            |$price CZK
            |$sizeInM2 m2
            |$locality
            |$url
        """.trimMargin()

    companion object {
        const val EMAIL_NAME_FROM = "Havasi Reality Watchers"
        const val EMAIL_ADDRESS_FROM = "reality@havasi.me"
        val EMAIL_FROM = EmailAddress(email = EMAIL_ADDRESS_FROM, name = EMAIL_NAME_FROM)
    }
}
