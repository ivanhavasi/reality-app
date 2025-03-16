package cz.havasi.repository.entity

import cz.havasi.repository.entity.enum.UserRoleEntity
import io.quarkus.runtime.annotations.RegisterForReflection
import org.bson.types.ObjectId
import java.time.OffsetDateTime

@RegisterForReflection
internal data class UserEntity(
    val _id: ObjectId,
    val googleId: String,
    val email: String,
    val username: String,
    val roles: Set<UserRoleEntity>,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)
