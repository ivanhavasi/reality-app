package cz.havasi.reality.app.service.util

import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.Locality
import java.util.*

public fun constructFingerprint(
    buildingType: BuildingType,
    locality: Locality,
    subCategory: String,
): String = "${buildingType.name.lowercase(Locale.FRANCE)}-${locality.city}-${locality.street ?: ""}-$subCategory"
// todo create a new fingerprint and hash it
