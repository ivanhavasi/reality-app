package cz.havasi.reality.app.mongo.entity

import io.quarkus.runtime.annotations.RegisterForReflection
import org.bson.types.ObjectId
import java.time.OffsetDateTime

@RegisterForReflection
internal data class FavoriteEntity(
    val _id: ObjectId,
    val userId: ObjectId,
    val listingId: String, // Apartment.id, the stored listing's externalId
    val createdAt: OffsetDateTime,
)
