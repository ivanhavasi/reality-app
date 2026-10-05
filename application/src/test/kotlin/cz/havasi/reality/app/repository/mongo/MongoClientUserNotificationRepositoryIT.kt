package cz.havasi.reality.app.repository.mongo

import com.mongodb.client.model.Filters
import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.FilterRange
import cz.havasi.reality.app.model.NotificationFilter
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.model.command.AddUserNotificationCommand
import cz.havasi.reality.app.model.command.EmailNotificationCommand
import cz.havasi.reality.app.model.command.FindUserNotificationsForFilterCommand
import cz.havasi.reality.app.model.command.UpdateUserNotificationCommand
import cz.havasi.reality.app.repository.mongo.TestDatabaseNames.TEST_DB_NAME
import cz.havasi.reality.app.service.repository.UserNotificationRepository
import io.quarkus.mongodb.reactive.ReactiveMongoClient
import io.quarkus.test.junit.QuarkusTest
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.inject.Inject
import kotlinx.coroutines.test.runTest
import org.bson.Document
import org.bson.types.ObjectId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@QuarkusTest
internal class MongoClientUserNotificationRepositoryIT {
    @Inject
    lateinit var mongoReactiveClient: ReactiveMongoClient

    @Inject
    lateinit var userNotificationRepository: UserNotificationRepository

    private val userId = ObjectId.get().toHexString()
    private val otherUserId = ObjectId.get().toHexString()

    // only this test's users: the collection name is shared with production
    @AfterEach
    fun cleanUp() = runTest {
        mongoReactiveClient.getDatabase(TEST_DB_NAME)
            .getCollection(NOTIFICATIONS, Document::class.java)
            .deleteMany(Filters.`in`("userId", listOf(ObjectId(userId), ObjectId(otherUserId))))
            .awaitSuspending()
    }

    private suspend fun addNotification(
        buildingType: BuildingType,
        subTypes: List<String>?,
        owner: String = userId,
    ): String =
        userNotificationRepository.addUserNotification(
            AddUserNotificationCommand(
                owner,
                EmailNotificationCommand(
                    name = "$buildingType $subTypes",
                    filter = NotificationFilter(
                        buildingType = buildingType,
                        transactionType = TransactionType.SALE,
                        size = FilterRange(500.0, 1_500.0),
                        price = FilterRange(null, 15_000_000),
                        subTypes = subTypes,
                    ),
                    email = "it@example.com",
                ),
            ),
        )

    private suspend fun matchingIdsForPlot(): Set<String> =
        userNotificationRepository.findUserNotificationsForFilter(
            FindUserNotificationsForFilterCommand(
                buildingType = BuildingType.LAND,
                transactionType = TransactionType.SALE,
                size = 954.0,
                price = 10_000_000,
                subTypes = listOf("BUILDING_PLOT"),
            ),
        )
            .filter { it.userId == userId }
            .map { it.id }
            .toSet()

    @Test
    fun `land building plot filter matches a building plot`() = runTest {
        val id = addNotification(BuildingType.LAND, listOf("BUILDING_PLOT"))

        assertEquals(setOf(id), matchingIdsForPlot())
    }

    @Test
    fun `apartment filter does not match land`() = runTest {
        addNotification(BuildingType.APARTMENT, listOf("2+kk"))
        addNotification(BuildingType.APARTMENT, emptyList())

        assertEquals(emptySet<String>(), matchingIdsForPlot())
    }

    @Test
    fun `empty subTypes matches every subtype`() = runTest {
        val id = addNotification(BuildingType.LAND, emptyList())
        addNotification(BuildingType.LAND, listOf("GARDEN"))

        assertEquals(setOf(id), matchingIdsForPlot())
    }

    @Test
    fun `remove, enable and disable only act on the owner's notification`() = runTest {
        val id = addNotification(BuildingType.LAND, listOf("BUILDING_PLOT"))

        assertFalse(userNotificationRepository.updateUserNotification(UpdateUserNotificationCommand(otherUserId, id, enabled = false)))
        assertFalse(userNotificationRepository.removeUserNotification(otherUserId, id))
        assertEquals(setOf(id), matchingIdsForPlot())

        assertTrue(userNotificationRepository.updateUserNotification(UpdateUserNotificationCommand(userId, id, enabled = false)))
        assertEquals(emptySet<String>(), matchingIdsForPlot())
        assertTrue(userNotificationRepository.removeUserNotification(userId, id))
    }
}

private const val NOTIFICATIONS = "notifications"
