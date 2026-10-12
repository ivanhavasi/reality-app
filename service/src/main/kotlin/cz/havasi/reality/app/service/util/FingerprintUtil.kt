package cz.havasi.reality.app.service.util

import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.Locality
import cz.havasi.reality.app.model.TransactionType
import java.util.*
import kotlin.math.roundToInt

// land and houses drop street and subtype: street is often missing and portals classify the same property differently.
// A house is keyed by usable area only: its plot area is missing or a "1 m²" placeholder on some portals
public fun constructFingerprint(
    buildingType: BuildingType,
    locality: Locality,
    subCategory: String,
    transactionType: TransactionType,
    sizeInM2: Double? = null,
): String = when (buildingType) {
    BuildingType.LAND ->
        "land-${locality.city}-${transactionType.name}-${checkNotNull(sizeInM2) { "Land fingerprint needs size" }.roundToInt()}"
    BuildingType.HOUSE ->
        "house-${locality.city}-${transactionType.name}-${checkNotNull(sizeInM2) { "House fingerprint needs size" }.roundToInt()}"
    BuildingType.APARTMENT ->
        "${buildingType.name.lowercase(Locale.FRANCE)}-${locality.city}-${locality.street ?: ""}-$subCategory-${transactionType.name}"
}
// todo create a new fingerprint and hash it
