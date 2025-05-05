package cz.havasi.reality.app.mongo.migration

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.Updates
import com.mongodb.client.model.WriteModel
import cz.havasi.reality.app.mongo.DatabaseNames.APARTMENT_COLLECTION_NAME
import cz.havasi.reality.app.mongo.DatabaseNames.USER_COLLECTION_NAME
import cz.havasi.reality.app.mongo.DatabaseNames.USER_NOTIFICATION_COLLECTION_NAME
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import io.quarkus.logging.Log
import kotlinx.coroutines.runBlocking
import org.bson.Document
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
import java.util.*

@ChangeUnit(
    id = "001_FixOffsetDateTimeFormat",
    order = "001",
    author = "ivan_havasi",
)
internal class FixOffsetDateTimeFormat {
    @Execution
    internal fun migration(mongoDatabase: MongoDatabase): Unit = runBlocking {
        val apartments = mongoDatabase.getCollection(APARTMENT_COLLECTION_NAME)
        val users = mongoDatabase.getCollection(USER_COLLECTION_NAME)
        val notifications = mongoDatabase.getCollection(USER_NOTIFICATION_COLLECTION_NAME)

        updateFieldToDateType(apartments, CREATED_AT)
        updateFieldToDateType(apartments, UPDATED_AT)
        updateFieldToDateType(users, CREATED_AT)
        updateFieldToDateType(users, UPDATED_AT)
        updateFieldToDateType(notifications, CREATED_AT)
        updateFieldToDateType(notifications, UPDATED_AT)
    }

    private fun updateFieldToDateType(collection: MongoCollection<Document>, fieldName: String) {
        val updates = mutableListOf<WriteModel<Document>>()
        val documents = collection.find()

        for (document in documents) {
            val id = document.getObjectId(ID)
            try {
                val field = OffsetDateTime.parse(document.getString(fieldName), ISO_OFFSET_DATE_TIME)

                updates.add(
                    UpdateOneModel<Document>(
                        Filters.eq(ID, id),
                        Updates.combine(
                            Updates.set(fieldName, field.convertToDate()),
                        ),
                    ),
                )
            } catch (e: Exception) {
                Log.error("Failed to parse date for doc $id: $e")
            }
        }

        if (updates.isNotEmpty()) {
            collection.bulkWrite(updates)
        }
    }

    @RollbackExecution
    internal fun rollback(mongoDatabase: MongoDatabase) {
        // No rollback
    }

    private fun OffsetDateTime.convertToDate() = Date.from(toInstant())
}

private const val CREATED_AT = "createdAt"
private const val UPDATED_AT = "updatedAt"
private const val ID = "_id"
