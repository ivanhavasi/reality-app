package cz.havasi.reality.app

import com.mongodb.client.model.Filters
import cz.havasi.reality.app.mongo.entity.ApartmentEntity
import cz.havasi.reality.app.mongo.repository.MongoClientApartmentRepository
import cz.havasi.reality.app.repository.mongo.TestDatabaseNames.TEST_APARTMENT_COLLECTION_NAME
import cz.havasi.reality.app.repository.mongo.TestDatabaseNames.TEST_DB_NAME
import io.quarkus.mongodb.reactive.ReactiveMongoClient
import io.quarkus.mongodb.reactive.ReactiveMongoCollection
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.inject.Inject
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

internal abstract class AbstractIT {
    @Inject
    lateinit var mongoReactiveClient: ReactiveMongoClient
    @Inject
    lateinit var apartmentRepository: MongoClientApartmentRepository
    lateinit var apartmentCollection: ReactiveMongoCollection<ApartmentEntity>

    @BeforeEach
    fun setup() = runTest {
        apartmentCollection = mongoReactiveClient.getDatabase(TEST_DB_NAME)
            .getCollection(TEST_APARTMENT_COLLECTION_NAME, ApartmentEntity::class.java)
    }

    @AfterEach
    fun cleanUp() = runTest {
        apartmentCollection.deleteMany(Filters.empty()).awaitSuspending()
    }
}