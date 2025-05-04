package cz.havasi.reality.app.model

import cz.havasi.reality.app.model.type.ProviderType

public data class Apartment(
    val id: String,
    val fingerprint: String,
    val name: String,
    val url: String,
    val price: Double,
    val pricePerM2: Double?,
    val sizeInM2: Double,
    val currency: cz.havasi.reality.app.model.CurrencyType,
    val locality: cz.havasi.reality.app.model.Locality,
    val mainCategory: cz.havasi.reality.app.model.BuildingType,
    val subCategory: String?,
    val transactionType: cz.havasi.reality.app.model.TransactionType,
    val images: List<String> = emptyList<String>(),
    val description: String? = null,
    val provider: ProviderType = ProviderType.UNKNOWN,
    val duplicates: List<cz.havasi.reality.app.model.ApartmentDuplicate> = listOf(),
)

public data class ApartmentDuplicate(
    val url: String,
    val price: Double,
    val pricePerM2: Double?,
    val images: List<String> = emptyList<String>(),
    val provider: ProviderType = ProviderType.UNKNOWN,
)

public data class Locality(
    val city: String,
    val district: String?,
    val street: String?,
    val streetNumber: String?,
    val latitude: Double?,
    val longitude: Double?,
)

public enum class CurrencyType {
    CZK,
    EUR,
    USD,
}

public enum class BuildingType {
    APARTMENT,
    HOUSE,
    LAND,
}

public enum class TransactionType {
    SALE,
    RENT,
}
