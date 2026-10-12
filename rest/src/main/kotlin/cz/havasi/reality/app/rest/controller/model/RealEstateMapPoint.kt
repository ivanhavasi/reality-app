package cz.havasi.reality.app.rest.controller.model

import cz.havasi.reality.app.model.Apartment
import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.CurrencyType
import cz.havasi.reality.app.model.Locality
import io.quarkus.runtime.annotations.RegisterForReflection

@RegisterForReflection
internal data class RealEstateMapPoint(
    val id: String,
    val name: String,
    val price: Double,
    val lowestPrice: Double,
    val pricePerM2: Double?,
    val sizeInM2: Double,
    val currency: CurrencyType,
    val mainCategory: BuildingType,
    val subCategory: String?,
    val locality: Locality,
    val image: String?,
)

// price 0 means "price on request" and must not win as the lowest price
internal fun Apartment.toMapPoint(): RealEstateMapPoint {
    val duplicatePrices = duplicates.map { it.price }.filter { it > 0 }
    return RealEstateMapPoint(
        id = id,
        name = name,
        price = price,
        lowestPrice = if (price <= 0 || duplicatePrices.isEmpty()) price else minOf(price, duplicatePrices.min()),
        pricePerM2 = pricePerM2,
        sizeInM2 = sizeInM2,
        currency = currency,
        mainCategory = mainCategory,
        subCategory = subCategory,
        locality = locality,
        image = images.firstOrNull(),
    )
}
