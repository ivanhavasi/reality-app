package cz.havasi.reality.app.rest.util

import cz.havasi.reality.app.model.Apartment
import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.Locality
import cz.havasi.reality.app.model.type.LandSubCategory
import cz.havasi.reality.app.service.util.firstCapitalOthersLowerCase

internal fun Apartment.buildingTypeLabel(): String =
    mainCategory.name.firstCapitalOthersLowerCase()

internal fun Apartment.typeLabel(): String =
    when (mainCategory) {
        BuildingType.LAND -> LandSubCategory.fromValueOrNull(subCategory)?.label ?: "Land"
        else -> listOfNotNull(buildingTypeLabel(), subCategory).joinToString(" ")
    }

internal fun Locality.toDisplayString(): String =
    listOfNotNull(street, district, city).filter { it.isNotBlank() }.distinct().joinToString(", ")
