package cz.havasi.reality.app.mongo.migration

import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import cz.havasi.reality.app.mongo.DatabaseNames.SENT_NOTIFICATION_COLLECTION_NAME
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

@ChangeUnit(
    id = "002_AddTtlToSentNotification",
    order = "002",
    author = "ivan_havasi",
)
internal class AddTtlToSentNotification {
    @Execution
    internal fun migration(mongoDatabase: MongoDatabase): Unit = runBlocking {
        val sentNotifications = mongoDatabase.getCollection(SENT_NOTIFICATION_COLLECTION_NAME)
        val options = IndexOptions().expireAfter(30, TimeUnit.DAYS).name(INDEX_NAME)

        sentNotifications.createIndex(
            Indexes.ascending(SENT_AT),
            options,
        )
    }

    @RollbackExecution
    internal fun rollback(mongoDatabase: MongoDatabase): Unit = runBlocking {
        val sentNotifications = mongoDatabase.getCollection(SENT_NOTIFICATION_COLLECTION_NAME)

        sentNotifications.dropIndex(INDEX_NAME)
    }
}

private const val INDEX_NAME = "sentAt_ttl"
private const val SENT_AT = "sentAt"
