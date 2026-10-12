package cz.havasi.reality.app.repository.mongo

import com.mongodb.client.MongoClient
import com.mongodb.client.model.Filters
import cz.havasi.reality.app.model.command.CreateUserCommand
import cz.havasi.reality.app.mongo.migration.MERGED_USERS_BACKUP_COLLECTION_NAME
import cz.havasi.reality.app.mongo.migration.mergeDuplicateUsers
import cz.havasi.reality.app.repository.mongo.TestDatabaseNames.TEST_DB_NAME
import cz.havasi.reality.app.service.repository.UserRepository
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import org.bson.Document
import org.bson.types.ObjectId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@QuarkusTest
internal class MongoClientUserRepositoryIT {
    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var mongoClient: MongoClient

    private val email = "it-${ObjectId.get().toHexString()}@example.com"
    private val users get() = mongoClient.getDatabase(TEST_DB_NAME).getCollection("users")
    private val mergeDatabase get() = mongoClient.getDatabase(MERGE_DB_NAME)

    // only this test's data: the users collection name is shared with production
    @AfterEach
    fun cleanUp() {
        users.deleteMany(Filters.eq("email", email))
        mergeDatabase.drop()
    }

    @Test
    fun `parallel first requests of a new user create one user`() = runTest {
        val command = CreateUserCommand(email = email, username = "it", googleId = "google-it")

        val ids = coroutineScope {
            (1..10).map { async(Dispatchers.IO) { userRepository.save(command) } }.awaitAll()
        }

        assertEquals(1, ids.toSet().size)
        assertEquals(1L, users.countDocuments(Filters.eq("email", email)))
    }

    @Test
    fun `merging keeps the admin, moves alerts over and backs up the removed user`() {
        val olderId = ObjectId.get()
        val adminId = ObjectId.get()
        val otherId = ObjectId.get()
        val mergeUsers = mergeDatabase.getCollection("users")
        mergeUsers.insertMany(
            listOf(
                Document("_id", olderId).append("email", "same@example.com").append("roles", listOf("USER")),
                Document("_id", adminId).append("email", "same@example.com").append("roles", listOf("ADMIN")),
                Document("_id", otherId).append("email", "other@example.com").append("roles", listOf("USER")),
            ),
        )
        mergeDatabase.getCollection("notifications").insertMany(listOf(Document("userId", olderId), Document("userId", adminId)))
        mergeDatabase.getCollection("sentNotifications").insertOne(Document("userId", olderId))

        val removed = mergeDuplicateUsers(mergeDatabase)

        assertEquals(1, removed)
        assertEquals(setOf(adminId, otherId), mergeUsers.find().map { it.getObjectId("_id") }.toSet())
        assertEquals(setOf("USER", "ADMIN"), mergeUsers.find(Filters.eq("_id", adminId)).first()!!.getList("roles", String::class.java).toSet())
        assertTrue(mergeDatabase.getCollection("notifications").find().all { it.getObjectId("userId") == adminId })
        assertEquals(adminId, mergeDatabase.getCollection("sentNotifications").find().first()!!.getObjectId("userId"))
        assertEquals(listOf(olderId), mergeDatabase.getCollection(MERGED_USERS_BACKUP_COLLECTION_NAME).find().map { it.getObjectId("_id") }.toList())
        assertEquals(0, mergeDuplicateUsers(mergeDatabase))
    }

    private companion object {
        const val MERGE_DB_NAME = "user_merge_it"
    }
}
