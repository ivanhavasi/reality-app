package cz.havasi.reality.app.mongo.migration

import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import cz.havasi.reality.app.mongo.DatabaseNames.PLACE_COLLECTION_NAME
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution

// $geoNear needs it; the category prefix keeps each nearest-place lookup to one category
@ChangeUnit(
    id = "010_PlaceIndex",
    order = "010",
    author = "ivan_havasi",
)
internal class PlaceIndex {
    @Execution
    internal fun migration(mongoDatabase: MongoDatabase) {
        mongoDatabase.getCollection(PLACE_COLLECTION_NAME).createIndex(
            Indexes.compoundIndex(Indexes.ascending(CATEGORY), Indexes.geo2dsphere(LOCATION)),
            IndexOptions().name(PLACE_INDEX_NAME),
        )
    }

    @RollbackExecution
    internal fun rollback(mongoDatabase: MongoDatabase) {
        mongoDatabase.getCollection(PLACE_COLLECTION_NAME).dropIndex(PLACE_INDEX_NAME)
    }
}

private const val CATEGORY = "category"
private const val LOCATION = "location"
private const val PLACE_INDEX_NAME = "category_location_2dsphere"
