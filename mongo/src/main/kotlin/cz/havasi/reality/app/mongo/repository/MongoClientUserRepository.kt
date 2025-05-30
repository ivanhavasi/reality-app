package cz.havasi.reality.app.mongo.repository

import com.mongodb.client.model.Filters
import cz.havasi.reality.app.model.User
import cz.havasi.reality.app.model.command.CreateUserCommand
import cz.havasi.reality.app.model.type.UserRole
import cz.havasi.reality.app.mongo.DatabaseNames.DB_NAME
import cz.havasi.reality.app.mongo.DatabaseNames.USER_COLLECTION_NAME
import cz.havasi.reality.app.mongo.entity.UserEntity
import cz.havasi.reality.app.mongo.entity.type.UserRoleEntity
import cz.havasi.reality.app.service.repository.UserRepository
import io.quarkus.mongodb.reactive.ReactiveMongoClient
import io.smallrye.mutiny.coroutines.asFlow
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.flow.firstOrNull
import org.bson.types.ObjectId
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC

@ApplicationScoped
internal class MongoClientUserRepository(
    private val reactiveMongoClient: ReactiveMongoClient,
) : UserRepository {
    private val mongoCollection =
        reactiveMongoClient.getDatabase(DB_NAME).getCollection(USER_COLLECTION_NAME, UserEntity::class.java)

    override suspend fun save(command: CreateUserCommand): String =
        mongoCollection.insertOne(command.toEntity())
            ?.awaitSuspending()
            ?.insertedId
            ?.asObjectId()
            ?.value
            ?.toHexString()
            ?: throw error("User with email ${command.email} was not saved into mongo db")

    override suspend fun getUserById(id: String): User =
        mongoCollection.find(Filters.eq("_id", ObjectId(id)), UserEntity::class.java)
            .asFlow()
            .firstOrNull()
            ?.toModel()
            ?: throw error("User with id $id was not found in mongo db")

    override suspend fun getUserByEmailOrNull(email: String): User? =
        mongoCollection.find(Filters.eq("email", email), UserEntity::class.java)
            .asFlow()
            .firstOrNull()
            ?.toModel()

    private fun UserEntity.toModel() =
        User(
            id = _id.toHexString(),
            googleId = googleId,
            email = email,
            username = username,
            createdAt = createdAt,
            updatedAt = updatedAt,
            roles = roles.map { UserRole.valueOf(it.name) }.toSet(),
        )

    private fun CreateUserCommand.toEntity() =
        UserEntity(
            _id = ObjectId.get(),
            email = email,
            username = username,
            createdAt = OffsetDateTime.now(UTC),
            updatedAt = OffsetDateTime.now(UTC),
            googleId = googleId,
            roles = roles.map { UserRoleEntity.valueOf(it.name) }.toSet(),
        )
}
