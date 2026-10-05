package cz.havasi.reality.app.service.util

import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.Locality
import cz.havasi.reality.app.model.TransactionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

internal class FingerprintUtilTest {
    private val locality = Locality(
        city = "Praha",
        district = "Radotín",
        street = "Výpadová",
        streetNumber = "12",
        latitude = 50.0,
        longitude = 14.3,
    )

    @Test
    fun `apartment fingerprint is unchanged`() {
        assertEquals(
            "apartment-Praha-Výpadová-2+kk-SALE",
            constructFingerprint(BuildingType.APARTMENT, locality, "2+kk", TransactionType.SALE),
        )
        assertEquals(
            "apartment-Praha-Výpadová-2+kk-SALE",
            constructFingerprint(BuildingType.APARTMENT, locality, "2+kk", TransactionType.SALE, 65.0),
        )
        assertEquals(
            "apartment-Praha--3+1-RENT",
            constructFingerprint(BuildingType.APARTMENT, locality.copy(street = null), "3+1", TransactionType.RENT),
        )
        assertEquals(
            "house-Brno-Lidická-5+1-SALE",
            constructFingerprint(BuildingType.HOUSE, locality.copy(city = "Brno", street = "Lidická"), "5+1", TransactionType.SALE),
        )
    }

    @Test
    fun `land fingerprint uses city, transaction and area only`() {
        assertEquals(
            "land-Praha-SALE-954",
            constructFingerprint(BuildingType.LAND, locality, "BUILDING_PLOT", TransactionType.SALE, 954.0),
        )
        assertEquals(
            "land-Praha-SALE-954",
            constructFingerprint(BuildingType.LAND, locality.copy(street = null, district = null), "", TransactionType.SALE, 954.2),
        )
    }

    @Test
    fun `land fingerprint requires size`() {
        assertThrows<IllegalStateException> {
            constructFingerprint(BuildingType.LAND, locality, "BUILDING_PLOT", TransactionType.SALE)
        }
    }
}
