package cz.havasi.reality.app.mongo.migration

import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import cz.havasi.reality.app.mongo.DatabaseNames.SENT_NOTIFICATION_COLLECTION_NAME
import cz.havasi.reality.app.mongo.DatabaseNames.USER_NOTIFICATION_COLLECTION_NAME
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import kotlinx.coroutines.runBlocking

@ChangeUnit(
    id = "003_NotificationIndexes",
    order = "003",
    author = "ivan_havasi",
)
internal class NotificationIndexes {
    @Execution
    internal fun migration(mongoDatabase: MongoDatabase): Unit = runBlocking {
        val sentNotifications = mongoDatabase.getCollection(SENT_NOTIFICATION_COLLECTION_NAME)
        val userNotifications = mongoDatabase.getCollection(USER_NOTIFICATION_COLLECTION_NAME)

        // sent notifications
        sentNotifications.createIndex(
            Indexes.compoundIndex(
                Indexes.ascending(USER_ID),
                Indexes.ascending(REAL_ESTATE_ID),
                Indexes.ascending(TYPE),
            ),
            IndexOptions().name(SENT_NOTIFICATION_INDEX_NAME),
        )
        // user notifications
        userNotifications.createIndex(
            Indexes.ascending(USER_ID),
            IndexOptions().name(USER_NOTIFICATION_USER_ID_INDEX_NAME),
        )
        userNotifications.createIndex(
            Indexes.compoundIndex(
                Indexes.ascending(ENABLED),
                Indexes.ascending(FILTER_BUILDING_TYPE),
                Indexes.ascending(FILTER_TRANSACTION_TYPE),
                Indexes.ascending(FILTER_SIZE_FROM),
                Indexes.ascending(FILTER_SIZE_TO),
                Indexes.ascending(FILTER_PRICE_FROM),
                Indexes.ascending(FILTER_PRICE_TO),
            ),
            IndexOptions().name(USER_NOTIFICATION_FILTER_INDEX_NAME),
        )
    }

    @RollbackExecution
    internal fun rollback(mongoDatabase: MongoDatabase): Unit = runBlocking {
        val sentNotifications = mongoDatabase.getCollection(SENT_NOTIFICATION_COLLECTION_NAME)
        val userNotifications = mongoDatabase.getCollection(USER_NOTIFICATION_COLLECTION_NAME)

        sentNotifications.dropIndex(SENT_NOTIFICATION_INDEX_NAME)
        userNotifications.dropIndex(USER_NOTIFICATION_USER_ID_INDEX_NAME)
        userNotifications.dropIndex(USER_NOTIFICATION_FILTER_INDEX_NAME)
    }
}

private const val SENT_NOTIFICATION_INDEX_NAME = "user_id_real_estate_type_1"
private const val USER_NOTIFICATION_USER_ID_INDEX_NAME = "userId_1"
private const val USER_NOTIFICATION_FILTER_INDEX_NAME = "notifications_filter_1"
private const val REAL_ESTATE_ID = "realEstate.id"
private const val TYPE = "type"
private const val USER_ID = "userId"
private const val ENABLED = "enabled"
private const val FILTER_BUILDING_TYPE = "filter.buildingType"
private const val FILTER_TRANSACTION_TYPE = "filter.transactionType"
private const val FILTER_SIZE_FROM = "filter.size.from"
private const val FILTER_SIZE_TO = "filter.size.to"
private const val FILTER_PRICE_FROM = "filter.price.from"
private const val FILTER_PRICE_TO = "filter.price.to"
