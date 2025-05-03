package cz.havasi.repository.migration

import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import cz.havasi.repository.DatabaseNames.SENT_NOTIFICATION_COLLECTION_NAME
import cz.havasi.repository.DatabaseNames.USER_NOTIFICATION_COLLECTION_NAME
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import kotlinx.coroutines.runBlocking

@ChangeUnit(
    id = "003_NotificationIndexes",
    order = "003",
    author = "ivan_havasi",
)
public class NotificationIndexes {
    @Execution
    public fun migration(mongoDatabase: MongoDatabase): Unit = runBlocking {
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
    public fun rollback(mongoDatabase: MongoDatabase): Unit = runBlocking {
        val sentNotifications = mongoDatabase.getCollection(SENT_NOTIFICATION_COLLECTION_NAME)
        val userNotifications = mongoDatabase.getCollection(USER_NOTIFICATION_COLLECTION_NAME)

        sentNotifications.dropIndex(SENT_NOTIFICATION_INDEX_NAME)
        userNotifications.dropIndex(USER_NOTIFICATION_USER_ID_INDEX_NAME)
        userNotifications.dropIndex(USER_NOTIFICATION_FILTER_INDEX_NAME)
    }

    private companion object {
        const val SENT_NOTIFICATION_INDEX_NAME = "user_id_real_estate_type_1"
        const val USER_NOTIFICATION_USER_ID_INDEX_NAME = "userId_1"
        const val USER_NOTIFICATION_FILTER_INDEX_NAME = "notifications_filter_1"
        const val REAL_ESTATE_ID = "realEstate.id"
        const val TYPE = "type"
        const val USER_ID = "userId"
        const val ENABLED = "enabled"
        const val FILTER_BUILDING_TYPE = "filter.buildingType"
        const val FILTER_TRANSACTION_TYPE = "filter.transactionType"
        const val FILTER_SIZE_FROM = "filter.size.from"
        const val FILTER_SIZE_TO = "filter.size.to"
        const val FILTER_PRICE_FROM = "filter.price.from"
        const val FILTER_PRICE_TO = "filter.price.to"
    }
}