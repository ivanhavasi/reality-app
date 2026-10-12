package cz.havasi.reality.app.service.util

import java.text.Normalizer

public fun String.firstCapitalOthersLowerCase(): String =
    lowercase().replaceFirstChar { it.uppercase() }

private val PRAGUE_NUMBERED_DISTRICT = Regex("""^Praha( \d+)?$""")
private val COMBINING_MARKS = Regex("""\p{M}+""")

// "Praha-Radotín" / "Radotín" -> "radotin"; "Praha 5" carries no city part -> null
internal fun String?.toComparableCityPart(): String? =
    this?.trim()?.removePrefix("Praha-")
        ?.takeUnless { PRAGUE_NUMBERED_DISTRICT.matches(it) }
        ?.toComparable()

// "U Hostivařského nádraží" / "U hostivařského nádraží" -> "u hostivarskeho nadrazi"
internal fun String?.toComparableStreet(): String? = this?.toComparable()

// any free text, e.g. a geocoder's place name, so city parts can be looked up in it
internal fun String?.toComparableText(): String? = this?.toComparable()

private fun String.toComparable(): String? =
    trim().takeUnless { it.isBlank() }
        ?.let { Normalizer.normalize(it, Normalizer.Form.NFD).replace(COMBINING_MARKS, "").lowercase() }
