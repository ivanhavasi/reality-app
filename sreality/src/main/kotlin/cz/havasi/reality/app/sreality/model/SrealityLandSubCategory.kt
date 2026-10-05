package cz.havasi.reality.app.sreality.model

import cz.havasi.reality.app.model.type.LandSubCategory

internal enum class SrealityLandSubCategory(
    val code: Int,
    val canonical: LandSubCategory,
    val detailSlug: String,
) {
    BUILDING_PLOT(19, LandSubCategory.BUILDING_PLOT, "bydleni"),
    COMMERCIAL(18, LandSubCategory.COMMERCIAL, "komercni"),
    FIELD(20, LandSubCategory.FIELD, "pole"),
    FOREST(21, LandSubCategory.FOREST, "les"),
    MEADOW(22, LandSubCategory.MEADOW, "louka"),
    GARDEN(23, LandSubCategory.GARDEN, "zahrada"),
    OTHER(24, LandSubCategory.OTHER, "ostatni-pozemky"),
    POND(46, LandSubCategory.POND, "rybnik"),
    ORCHARD_VINEYARD(48, LandSubCategory.ORCHARD_VINEYARD, "sady-vinice"),
    ;

    companion object {
        fun fromCode(code: String?): SrealityLandSubCategory? = entries.firstOrNull { it.code.toString() == code }

        fun from(canonical: LandSubCategory): SrealityLandSubCategory = entries.first { it.canonical == canonical }
    }
}
