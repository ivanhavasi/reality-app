package cz.havasi.reality.app.mongo.migration

import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import cz.havasi.reality.app.mongo.DatabaseNames.USER_COLLECTION_NAME
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import io.quarkus.logging.Log

// the index can't be built while duplicates exist, so they are merged first
@ChangeUnit(
    id = "007_UniqueUserEmail",
    order = "007",
    author = "ivan_havasi",
)
internal class UniqueUserEmail {
    @Execution
    internal fun migration(mongoDatabase: MongoDatabase) {
        val removed = mergeDuplicateUsers(mongoDatabase)
        Log.info("Merged $removed duplicate users, removed ones are backed up in $MERGED_USERS_BACKUP_COLLECTION_NAME")

        mongoDatabase.getCollection(USER_COLLECTION_NAME).createIndex(
            Indexes.ascending(EMAIL),
            IndexOptions().unique(true).name(USER_EMAIL_UNIQUE_INDEX_NAME),
        )
    }

    // only the index: merged users stay merged, the removed ones can be restored from the backup collection
    @RollbackExecution
    internal fun rollback(mongoDatabase: MongoDatabase) {
        mongoDatabase.getCollection(USER_COLLECTION_NAME).dropIndex(USER_EMAIL_UNIQUE_INDEX_NAME)
    }
}

private const val EMAIL = "email"
private const val USER_EMAIL_UNIQUE_INDEX_NAME = "email_unique_1"
