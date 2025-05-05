package cz.havasi.reality.app.mongo.migration.config

import com.mongodb.client.MongoClient
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
internal class MongoClientInitializer(
    private val mongoClient: MongoClient, // force quarkus to create and inject this bean, so mongock can use it, because mongock doesn't support reactive mongo client yet
)
