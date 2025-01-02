package cz.havasi.repository.entity

import org.bson.types.ObjectId
import java.time.OffsetDateTime

public data class ApartmentEntity(
    val _id: ObjectId,
    val externalId: String,
    val fingerprint: String,
    val name: String,
    val url: String,
    val price: Double,
    val pricePerM2: Double?,
    val sizeInM2: Double,
    val currency: String,
    val locality: LocalityEntity,
    val mainCategory: String,
    val subCategory: String?,
    val transactionType: String,
    val images: List<String> = emptyList(),
    val description: String? = null,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

public data class LocalityEntity(
    val city: String,
    val district: String?,
    val street: String?,
    val streetNumber: String?,
    val latitude: Double?,
    val longitude: Double?,
)
