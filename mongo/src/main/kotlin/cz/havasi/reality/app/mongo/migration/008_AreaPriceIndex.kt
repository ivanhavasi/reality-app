package cz.havasi.reality.app.mongo.migration

import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import cz.havasi.reality.app.mongo.DatabaseNames.APARTMENT_COLLECTION_NAME
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution

// every listing detail asks for the prices around it; without it each request scans all listings of the type
@ChangeUnit(
    id = "008_AreaPriceIndex",
    order = "008",
    author = "ivan_havasi",
)
internal class AreaPriceIndex {
    @Execution
    internal fun migration(mongoDatabase: MongoDatabase) {
        mongoDatabase.getCollection(APARTMENT_COLLECTION_NAME).createIndex(
            Indexes.compoundIndex(
                Indexes.ascending(MAIN_CATEGORY),
                Indexes.ascending(TRANSACTION_TYPE),
                Indexes.ascending(LATITUDE),
                Indexes.ascending(LONGITUDE),
            ),
            IndexOptions().name(AREA_PRICE_INDEX_NAME),
        )
    }

    @RollbackExecution
    internal fun rollback(mongoDatabase: MongoDatabase) {
        mongoDatabase.getCollection(APARTMENT_COLLECTION_NAME).dropIndex(AREA_PRICE_INDEX_NAME)
    }
}

private const val MAIN_CATEGORY = "mainCategory"
private const val TRANSACTION_TYPE = "transactionType"
private const val LATITUDE = "locality.latitude"
private const val LONGITUDE = "locality.longitude"
private const val AREA_PRICE_INDEX_NAME = "mainCategory_transactionType_latitude_longitude"
