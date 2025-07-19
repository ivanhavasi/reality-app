package cz.havasi.reality.app.mongo.migration

import com.mongodb.client.MongoDatabase
import cz.havasi.reality.app.mongo.DatabaseNames.APARTMENT_COLLECTION_NAME
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import kotlinx.coroutines.runBlocking
import org.bson.Document

@ChangeUnit(
    id = "005_UpdateFingerPrintWithTransaction",
    order = "005",
    author = "ivan_havasi",
)
public class UpdateFingerPrintWithTransaction {
    @Execution
    public fun migration(mongoDatabase: MongoDatabase): Unit = runBlocking {
        val apartments = mongoDatabase.getCollection(APARTMENT_COLLECTION_NAME)

        apartments.updateMany(
            Document(),
            listOf(
                Document(
                    "\$set",
                    Document("fingerprint", Document("\$concat", listOf("\$fingerprint", "-", "\$transactionType"))),
                ),
            ),
        )
    }

    @RollbackExecution
    public fun rollback(mongoDatabase: MongoDatabase): Unit = runBlocking {
        // no rollback
    }
}
