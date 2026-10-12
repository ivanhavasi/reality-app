package cz.havasi.reality.app.mongo.migration

import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Accumulators
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Updates
import cz.havasi.reality.app.mongo.DatabaseNames.SENT_NOTIFICATION_COLLECTION_NAME
import cz.havasi.reality.app.mongo.DatabaseNames.USER_COLLECTION_NAME
import cz.havasi.reality.app.mongo.DatabaseNames.USER_NOTIFICATION_COLLECTION_NAME
import org.bson.Document

public const val MERGED_USERS_BACKUP_COLLECTION_NAME: String = "usersMergedDuplicates"

// a new user's first requests ran in parallel and each created the user, so one email could own several users.
// Keeps one per email (an admin, otherwise the oldest) with the union of their roles, moves their alerts and sent
// alerts over, and backs the removed users up. Safe to run again after a partial failure; returns how many it removed
public fun mergeDuplicateUsers(database: MongoDatabase): Int {
    val users = database.getCollection(USER_COLLECTION_NAME)
    val duplicatedEmails = users.aggregate(
        listOf(
            Aggregates.match(Filters.type("email", "string")),
            Aggregates.group("\$email", Accumulators.sum("count", 1)),
            Aggregates.match(Filters.gt("count", 1)),
        ),
    ).map { it.getString("_id") }.toList()

    return duplicatedEmails.sumOf { email ->
        // ObjectIds grow with creation time, so after the admins the oldest user comes first
        val sameEmail = users.find(Filters.eq("email", email)).toList()
            .sortedWith(compareByDescending<Document> { ADMIN in it.roles() }.thenBy { it.getObjectId("_id") })
        val kept = sameEmail.first().getObjectId("_id")
        val removed = sameEmail.drop(1)
        val removedIds = removed.map { it.getObjectId("_id") }

        users.updateOne(Filters.eq("_id", kept), Updates.set("roles", sameEmail.flatMap { it.roles() }.distinct()))
        listOf(USER_NOTIFICATION_COLLECTION_NAME, SENT_NOTIFICATION_COLLECTION_NAME).forEach {
            database.getCollection(it).updateMany(Filters.`in`(USER_ID, removedIds), Updates.set(USER_ID, kept))
        }
        database.getCollection(MERGED_USERS_BACKUP_COLLECTION_NAME).bulkWrite(
            removed.map { ReplaceOneModel(Filters.eq("_id", it.getObjectId("_id")), it, ReplaceOptions().upsert(true)) },
        )
        users.deleteMany(Filters.`in`("_id", removedIds))
        removed.size
    }
}

private fun Document.roles(): List<String> = getList("roles", String::class.java) ?: emptyList()

private const val ADMIN = "ADMIN"
private const val USER_ID = "userId"
