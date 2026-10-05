package cz.havasi.reality.app.service.util

import java.text.Normalizer

public fun String.firstCapitalOthersLowerCase(): String =
    lowercase().replaceFirstChar { it.uppercase() }

private val PRAGUE_NUMBERED_DISTRICT = Regex("""^Praha( \d+)?$""")
private val COMBINING_MARKS = Regex("""\p{M}+""")

// "Praha-Radotín" / "Radotín" -> "radotin"; "Praha 5" carries no city part -> null
internal fun String?.toComparableCityPart(): String? =
    this?.trim()?.removePrefix("Praha-")
        ?.takeUnless { it.isBlank() || PRAGUE_NUMBERED_DISTRICT.matches(it) }
        ?.let { Normalizer.normalize(it, Normalizer.Form.NFD).replace(COMBINING_MARKS, "").lowercase() }
