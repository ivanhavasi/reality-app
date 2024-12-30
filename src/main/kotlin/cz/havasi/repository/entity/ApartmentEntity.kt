package cz.havasi.repository.entity

import io.quarkus.runtime.annotations.RegisterForReflection
import org.bson.codecs.pojo.annotations.BsonCreator
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.codecs.pojo.annotations.BsonProperty
import org.bson.types.ObjectId
import java.time.OffsetDateTime

@RegisterForReflection
public data class ApartmentEntity @BsonCreator constructor(
    @BsonId val id: ObjectId,
    @BsonProperty("externalId") val externalId: String,
    @BsonProperty("fingerprint") val fingerprint: String,
    @BsonProperty("name") val name: String,
    @BsonProperty("url") val url: String,
    @BsonProperty("price") val price: Double,
    @BsonProperty("pricePerM2") val pricePerM2: Double?,
    @BsonProperty("sizeInM2") val sizeInM2: Double,
    @BsonProperty("currency") val currency: String,
    @BsonProperty("locality") val locality: LocalityEntity,
    @BsonProperty("mainCategory") val mainCategory: String,
    @BsonProperty("subCategory") val subCategory: String?,
    @BsonProperty("transactionType") val transactionType: String,
    @BsonProperty("images") val images: List<String> = emptyList(),
    @BsonProperty("description") val description: String? = null,
    @BsonProperty("createdAt") val createdAt: OffsetDateTime,
    @BsonProperty("updatedAt") val updatedAt: OffsetDateTime,
)

@RegisterForReflection
public data class LocalityEntity @BsonCreator constructor(
    @BsonProperty("city") val city: String,
    @BsonProperty("district") val district: String?,
    @BsonProperty("street") val street: String?,
    @BsonProperty("streetNumber") val streetNumber: String?,
    @BsonProperty("latitude") val latitude: Double?,
    @BsonProperty("longitude") val longitude: Double?,
)
