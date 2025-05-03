package cz.havasi.repository.migration

import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import cz.havasi.repository.DatabaseNames.APARTMENT_COLLECTION_NAME
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import kotlinx.coroutines.runBlocking

@ChangeUnit(
    id = "004_ApartmentIndexes",
    order = "004",
    author = "ivan_havasi",
)
public class ApartmentIndexes {
    @Execution
    public fun migration(mongoDatabase: MongoDatabase): Unit = runBlocking {
        val apartments = mongoDatabase.getCollection(APARTMENT_COLLECTION_NAME)

        apartments.createIndex(
            Indexes.compoundIndex(
                Indexes.ascending(EXTERNAL_ID),
                Indexes.descending(CREATED_AT),
            ),
            IndexOptions().name(EXTERNAL_INDEX_NAME),
        )
        apartments.createIndex(
            Indexes.compoundIndex(
                Indexes.ascending(FINGERPRINT),
                Indexes.descending(CREATED_AT),
            ),
            IndexOptions().name(FINGERPRINT_INDEX_NAME),
        )
        apartments.createIndex(
            Indexes.ascending(UPDATED_AT),
            IndexOptions().name(UPDATED_AT_ASC_INDEX_NAME),
        )
        apartments.createIndex(
            Indexes.descending(UPDATED_AT),
            IndexOptions().name(UPDATED_AT_DESC_INDEX_NAME),
        )
    }

    @RollbackExecution
    public fun rollback(mongoDatabase: MongoDatabase): Unit = runBlocking {
        val apartments = mongoDatabase.getCollection(APARTMENT_COLLECTION_NAME)

        apartments.dropIndex(EXTERNAL_INDEX_NAME)
        apartments.dropIndex(FINGERPRINT_INDEX_NAME)
        apartments.dropIndex(UPDATED_AT_ASC_INDEX_NAME)
        apartments.dropIndex(UPDATED_AT_DESC_INDEX_NAME)
    }

    private companion object {
        const val EXTERNAL_ID = "externalId"
        const val FINGERPRINT = "fingerprint"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
        const val EXTERNAL_INDEX_NAME = "external_1"
        const val FINGERPRINT_INDEX_NAME = "fingerprint_1"
        const val UPDATED_AT_ASC_INDEX_NAME = "updatedAt_1"
        const val UPDATED_AT_DESC_INDEX_NAME = "updatedAt_-1"
    }
}