package cz.havasi.service

import cz.havasi.model.BuildingType
import cz.havasi.model.Locality
import java.util.Locale

public fun constructFingerprint(
    buildingType: BuildingType,
    locality: Locality,
    subCategory: String,
): String = "${buildingType.name.lowercase(Locale.FRANCE)}-${locality.city}-${locality.street ?: ""}-$subCategory"
