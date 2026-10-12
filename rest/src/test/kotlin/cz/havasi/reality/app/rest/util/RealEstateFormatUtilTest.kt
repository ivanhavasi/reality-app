package cz.havasi.reality.app.rest.util

import cz.havasi.reality.app.model.BuildingType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class RealEstateFormatUtilTest {
    @Test
    fun `typeLabel uses the land subtype label`() {
        assertEquals("Building plot", LAND_PLOT.typeLabel())
        assertEquals("Land", LAND_PLOT.copy(subCategory = null).typeLabel())
        assertEquals("Land", LAND_PLOT.copy(subCategory = "Bydlení").typeLabel())
    }

    @Test
    fun `typeLabel uses the house subtype label`() {
        val house = APARTMENT.copy(mainCategory = BuildingType.HOUSE, subCategory = "VILLA")

        assertEquals("Villa", house.typeLabel())
        assertEquals("House", house.copy(subCategory = null).typeLabel())
        assertEquals("House", house.copy(subCategory = "5+kk").typeLabel())
    }

    @Test
    fun `typeLabel keeps the apartment disposition`() {
        assertEquals("Apartment 2+kk", APARTMENT.typeLabel())
        assertEquals("Apartment", APARTMENT.copy(subCategory = null).typeLabel())
    }

    @Test
    fun `toDisplayString skips missing parts`() {
        assertEquals("Radotín, Praha", LAND_PLOT.locality.toDisplayString())
        assertEquals("Havlíčkova, Žižkov, Praha", APARTMENT.locality.toDisplayString())
        assertEquals("Praha", LAND_PLOT.locality.copy(district = "Praha").toDisplayString())
        assertEquals("Praha", LAND_PLOT.locality.copy(district = " ").toDisplayString())
    }
}
