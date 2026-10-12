package cz.havasi.reality.app.rest.util

import cz.havasi.reality.app.model.Apartment
import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.Locality
import cz.havasi.reality.app.model.type.HouseSubCategory
import cz.havasi.reality.app.model.type.LandSubCategory
import cz.havasi.reality.app.service.util.firstCapitalOthersLowerCase

internal fun Apartment.buildingTypeLabel(): String =
    mainCategory.name.firstCapitalOthersLowerCase()

internal fun Apartment.typeLabel(): String =
    when (mainCategory) {
        BuildingType.LAND -> LandSubCategory.fromValueOrNull(subCategory)?.label ?: "Land"
        BuildingType.HOUSE -> HouseSubCategory.fromValueOrNull(subCategory)?.label ?: "House"
        BuildingType.APARTMENT -> listOfNotNull(buildingTypeLabel(), subCategory).joinToString(" ")
    }

// "156 m², plot 296 m²" for a house with a known plot, just the area otherwise
internal fun Apartment.sizeLabel(): String =
    listOfNotNull(
        "${sizeInM2.formatToNumberWithSpaces()} m²",
        landSizeInM2?.let { "plot ${it.formatToNumberWithSpaces()} m²" },
    ).joinToString(", ")

internal fun Locality.toDisplayString(): String =
    listOfNotNull(street, district, city).filter { it.isNotBlank() }.distinct().joinToString(", ")
