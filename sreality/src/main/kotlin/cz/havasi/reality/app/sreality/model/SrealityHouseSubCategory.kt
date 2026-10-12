package cz.havasi.reality.app.sreality.model

import cz.havasi.reality.app.model.type.HouseSubCategory

internal enum class SrealityHouseSubCategory(
    val code: Int,
    val canonical: HouseSubCategory,
    val detailSlug: String,
) {
    FAMILY_HOUSE(37, HouseSubCategory.FAMILY_HOUSE, "rodinny"),
    VILLA(39, HouseSubCategory.VILLA, "vila"),
    COTTAGE(33, HouseSubCategory.COTTAGE, "chata"),
    COUNTRY_COTTAGE(43, HouseSubCategory.COTTAGE, "chalupa"),
    MULTI_GENERATIONAL(54, HouseSubCategory.MULTI_GENERATIONAL, "vicegeneracni-dum"),
    FARMSTEAD(44, HouseSubCategory.FARMSTEAD, "zemedelska-usedlost"),
    TURNKEY(40, HouseSubCategory.TURNKEY, "na-klic"),
    MONUMENT_OTHER(35, HouseSubCategory.OTHER, "pamatka"),
    ;

    companion object {
        fun fromCode(code: String?): SrealityHouseSubCategory? = entries.firstOrNull { it.code.toString() == code }
    }
}
