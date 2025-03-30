package cz.havasi.repository.entity

import cz.havasi.repository.entity.enum.ProviderTypeEntity
import io.quarkus.runtime.annotations.RegisterForReflection
import org.bson.types.ObjectId
import java.time.OffsetDateTime

@RegisterForReflection
public data class ApartmentEntity(
    val _id: ObjectId, // custom id for MongoDB
    val externalId: String, // is the same as Apartment.id
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
    val provider: ProviderTypeEntity = ProviderTypeEntity.UNKNOWN,
    val duplicates: List<ApartmentDuplicateEntity> = listOf(),
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

@RegisterForReflection
public data class ApartmentDuplicateEntity(
    val url: String,
    val price: Double,
    val pricePerM2: Double?,
    val images: List<String> = emptyList<String>(),
    val provider: ProviderTypeEntity = ProviderTypeEntity.UNKNOWN,
)

@RegisterForReflection
public data class LocalityEntity(
    val city: String,
    val district: String?,
    val street: String?,
    val streetNumber: String?,
    val latitude: Double?,
    val longitude: Double?,
)
