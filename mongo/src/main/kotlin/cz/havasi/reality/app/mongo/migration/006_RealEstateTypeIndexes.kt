package cz.havasi.reality.app.mongo.migration

import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import cz.havasi.reality.app.mongo.DatabaseNames.APARTMENT_COLLECTION_NAME
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import kotlinx.coroutines.runBlocking

@ChangeUnit(
    id = "006_RealEstateTypeIndexes",
    order = "006",
    author = "ivan_havasi",
)
public class RealEstateTypeIndexes {
    @Execution
    public fun migration(mongoDatabase: MongoDatabase): Unit = runBlocking {
        val apartments = mongoDatabase.getCollection(APARTMENT_COLLECTION_NAME)

        // without it the duplicates.id branch of findByIdOrFingerprint turns every dedup lookup into a collection scan
        apartments.createIndex(
            Indexes.ascending(DUPLICATE_ID),
            IndexOptions().name(DUPLICATE_ID_INDEX_NAME),
        )
        apartments.createIndex(
            Indexes.compoundIndex(
                Indexes.ascending(MAIN_CATEGORY),
                Indexes.ascending(TRANSACTION_TYPE),
                Indexes.descending(UPDATED_AT),
            ),
            IndexOptions().name(TYPE_UPDATED_AT_INDEX_NAME),
        )
    }

    @RollbackExecution
    public fun rollback(mongoDatabase: MongoDatabase): Unit = runBlocking {
        val apartments = mongoDatabase.getCollection(APARTMENT_COLLECTION_NAME)

        apartments.dropIndex(DUPLICATE_ID_INDEX_NAME)
        apartments.dropIndex(TYPE_UPDATED_AT_INDEX_NAME)
    }
}

private const val DUPLICATE_ID = "duplicates.id"
private const val MAIN_CATEGORY = "mainCategory"
private const val TRANSACTION_TYPE = "transactionType"
private const val UPDATED_AT = "updatedAt"
private const val DUPLICATE_ID_INDEX_NAME = "duplicates.id_1"
private const val TYPE_UPDATED_AT_INDEX_NAME = "mainCategory_transactionType_updatedAt_-1"
