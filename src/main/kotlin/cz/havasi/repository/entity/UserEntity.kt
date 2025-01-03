package cz.havasi.repository.entity

import io.quarkus.runtime.annotations.RegisterForReflection
import org.bson.types.ObjectId
import java.time.OffsetDateTime

@RegisterForReflection
public data class UserEntity(
    val _id: ObjectId,
    val email: String,
    val username: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)
