package cz.havasi.reality.app.service.util

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class StringUtilTest {
    @Test
    fun `toComparableCityPart strips the Praha prefix and diacritics`() {
        assertEquals("radotin", "Praha-Radotín".toComparableCityPart())
        assertEquals("radotin", "Radotín".toComparableCityPart())
        assertEquals("radotin", " radotín ".toComparableCityPart())
        assertEquals("dolni chabry", "Dolní Chabry".toComparableCityPart())
        assertEquals("modrany", "Modřany".toComparableCityPart())
        assertEquals("ujezd nad lesy", "Praha-Újezd nad Lesy".toComparableCityPart())
    }

    @Test
    fun `toComparableCityPart treats a numbered district as no city part`() {
        assertNull("Praha 5".toComparableCityPart())
        assertNull("Praha".toComparableCityPart())
        assertNull("".toComparableCityPart())
        assertNull("  ".toComparableCityPart())
        assertNull((null as String?).toComparableCityPart())
    }

    @Test
    fun `toComparableStreet ignores case, diacritics and blanks`() {
        assertEquals("u hostivarskeho nadrazi", "U Hostivařského nádraží".toComparableStreet())
        assertEquals("u hostivarskeho nadrazi", " u hostivařského nádraží ".toComparableStreet())
        assertNull(" ".toComparableStreet())
        assertNull((null as String?).toComparableStreet())
    }
}
