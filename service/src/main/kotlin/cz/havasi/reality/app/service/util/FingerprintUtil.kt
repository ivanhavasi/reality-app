package cz.havasi.reality.app.service.util

import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.Locality
import cz.havasi.reality.app.model.TransactionType
import java.util.*
import kotlin.math.roundToInt

// land drops street and subtype: street is missing on 29–41% of plots and portals classify the same plot differently
public fun constructFingerprint(
    buildingType: BuildingType,
    locality: Locality,
    subCategory: String,
    transactionType: TransactionType,
    sizeInM2: Double? = null,
): String = when (buildingType) {
    BuildingType.LAND ->
        "land-${locality.city}-${transactionType.name}-${checkNotNull(sizeInM2) { "Land fingerprint needs size" }.roundToInt()}"
    else ->
        "${buildingType.name.lowercase(Locale.FRANCE)}-${locality.city}-${locality.street ?: ""}-$subCategory-${transactionType.name}"
}
// todo create a new fingerprint and hash it
