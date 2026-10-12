package cz.havasi.reality.app.mongo.migration

import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import cz.havasi.reality.app.mongo.DatabaseNames.FAVORITE_COLLECTION_NAME
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution

// one favorite per user and listing; it also serves the user's list, which is sorted in memory
@ChangeUnit(
    id = "009_FavoriteIndex",
    order = "009",
    author = "ivan_havasi",
)
internal class FavoriteIndex {
    @Execution
    internal fun migration(mongoDatabase: MongoDatabase) {
        mongoDatabase.getCollection(FAVORITE_COLLECTION_NAME).createIndex(
            Indexes.compoundIndex(Indexes.ascending(USER_ID), Indexes.ascending(LISTING_ID)),
            IndexOptions().unique(true).name(FAVORITE_INDEX_NAME),
        )
    }

    @RollbackExecution
    internal fun rollback(mongoDatabase: MongoDatabase) {
        mongoDatabase.getCollection(FAVORITE_COLLECTION_NAME).dropIndex(FAVORITE_INDEX_NAME)
    }
}

private const val USER_ID = "userId"
private const val LISTING_ID = "listingId"
private const val FAVORITE_INDEX_NAME = "userId_listingId_unique"
