package cz.havasi.model

public data class Apartment(
    val id: String,
    val fingerprint: String,
    val name: String,
    val url: String,
    val price: Double,
    val pricePerM2: Double?,
    val sizeInM2: Double,
    val currency: CurrencyType,
    val locality: Locality,
    val mainCategory: BuildingType,
    val subCategory: String?,
    val transactionType: TransactionType,
    val images: List<String> = emptyList<String>(),
    val description: String? = null,
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
