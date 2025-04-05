package cz.havasi.repository.migration.config

import com.mongodb.client.MongoClient
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
public class MongoClientInitializer(
    private val mongoClient: MongoClient, // force quarkus to create and inject this bean, so mongock can use it, because mongock doesn't support reactive mongo client yet
)
