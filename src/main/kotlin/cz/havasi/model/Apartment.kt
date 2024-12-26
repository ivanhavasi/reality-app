package cz.havasi.model

internal data class Apartment(
    val id: Long,
    val name: String,
    val price: Double,
    val pricePerM2: Double?,
    val m2: Double?,
    val currency: String,
    val locality: Locality,
    val mainCategory: String,
    val subCategory: String,
    val transactionType: String,
    val images: List<String>,
    val description: String? = null,
    val createdAt: String? = null,
)

internal data class Locality(
    val city: String,
    val district: String?,
    val street: String?,
    val streetNumber: String?,
    val latitude: Double?,
    val longitude: Double?,
)

