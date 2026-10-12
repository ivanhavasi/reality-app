package cz.havasi.reality.app.service

import cz.havasi.reality.app.model.PlaceCategory
import cz.havasi.reality.app.model.PlacesImportProgress
import cz.havasi.reality.app.service.util.distanceInMetersTo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class PlaceImportServiceTest {
    private val repository = FakePlaceRepository()
    private val everyCategory = onePlaceInEveryCategory()

    private fun service(client: FakePlacesClient) = PlaceImportService(client, repository)

    @Test
    fun `every category is imported and progress reported`() = runTest {
        val reported = mutableListOf<PlacesImportProgress>()

        val result = service(FakePlacesClient(everyCategory)).importAll { reported += it }

        assertEquals(PlacesImportProgress(categoriesImported = 12, placesImported = 12), result)
        assertEquals(PlaceCategory.entries.toSet(), repository.stored.keys)
        assertEquals((1..12).toList(), reported.map { it.categoriesImported })
    }

    @Test
    fun `small parks are dropped and outlines get a point at least every 25 m`() = runTest {
        val parks = listOf(squarePark("pocket", 50.07, 14.43, 50.0), squarePark("large", 50.08, 14.44, 200.0))

        service(FakePlacesClient(everyCategory + (PlaceCategory.PARK to parks))).importAll()

        val stored = repository.stored.getValue(PlaceCategory.PARK).single()
        assertEquals("large", stored.id)
        val ring = stored.outerRings.single()
        assertEquals(33, ring.size) // 4 sides of 200 m in 8 steps each, plus the closing point
        assertTrue(ring.zipWithNext().all { (from, to) -> from.distanceInMetersTo(to) <= 25.0 })
    }

    @Test
    fun `busy server is retried`() = runTest {
        val client = FakePlacesClient(everyCategory, failures = mapOf(PlaceCategory.TRAM to 2))

        service(client).importAll()

        assertEquals(3, client.asked.count { it == PlaceCategory.TRAM })
        assertEquals(1, repository.stored.getValue(PlaceCategory.TRAM).size)
    }

    @Test
    fun `category that keeps failing or comes back empty keeps its places, the rest are updated`() = runTest {
        val previousTrams = listOf(point("old-tram", PlaceCategory.TRAM, 50.0, 14.0))
        repository.stored[PlaceCategory.TRAM] = previousTrams
        val client = FakePlacesClient(everyCategory - PlaceCategory.CINEMA, failures = mapOf(PlaceCategory.TRAM to Int.MAX_VALUE))

        val error = assertFailsWith<PlacesImportException> { service(client).importAll() }

        assertEquals("Import failed for TRAM, CINEMA, the other categories were updated", error.message)
        assertEquals(previousTrams, repository.stored[PlaceCategory.TRAM])
        assertEquals(PlaceCategory.entries.toSet() - PlaceCategory.CINEMA, repository.stored.keys)
    }
}
