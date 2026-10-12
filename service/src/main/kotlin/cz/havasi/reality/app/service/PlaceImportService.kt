package cz.havasi.reality.app.service

import cz.havasi.reality.app.model.Coordinates
import cz.havasi.reality.app.model.Place
import cz.havasi.reality.app.model.PlaceCategory
import cz.havasi.reality.app.model.PlacesImportProgress
import cz.havasi.reality.app.service.client.PlacesClient
import cz.havasi.reality.app.service.repository.PlaceRepository
import cz.havasi.reality.app.service.util.areaInSquareMeters
import cz.havasi.reality.app.service.util.densified
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

public class PlacesImportException(failed: List<PlaceCategory>) :
    IllegalStateException("Import failed for ${failed.joinToString()}, the other categories were updated")

// copies places from OpenStreetMap; the public Overpass server is busy at times, so each category is retried
// and one that keeps failing (or comes back empty) keeps the places it had
@ApplicationScoped
public class PlaceImportService(
    private val placesClient: PlacesClient,
    private val placeRepository: PlaceRepository,
) {
    private val mutex = Mutex()

    // tests on real time set these to zero
    internal var pauseBetweenCategoriesMillis: Long = 2_000L
    // busy evenings answer 504 for minutes at a time; the last delay is never waited for, the category is given up
    internal var retryDelaysMillis: List<Long> = listOf(30_000L, 60_000L, 120_000L, 240_000L, 0L)

    public suspend fun importAll(onProgress: (PlacesImportProgress) -> Unit = {}): PlacesImportProgress = mutex.withLock {
        var progress = PlacesImportProgress()
        val failed = mutableListOf<PlaceCategory>()
        PlaceCategory.entries.forEachIndexed { index, category ->
            if (index > 0) delay(pauseBetweenCategoriesMillis)
            val places = fetchWithRetries(category)
            if (places.isNullOrEmpty()) {
                failed += category
                return@forEachIndexed
            }
            placeRepository.replaceCategory(category, places)
            progress = progress.copy(
                categoriesImported = progress.categoriesImported + 1,
                placesImported = progress.placesImported + places.size,
            )
            onProgress(progress)
        }
        if (failed.isNotEmpty()) throw PlacesImportException(failed)
        progress
    }

    private suspend fun fetchWithRetries(category: PlaceCategory): List<Place>? {
        retryDelaysMillis.forEachIndexed { attempt, retryDelay ->
            try {
                return placesClient.findPlaces(category, IMPORT_AREA_SOUTH_WEST, IMPORT_AREA_NORTH_EAST).prepared(category)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.warn("Fetching places $category failed (attempt ${attempt + 1}): ${e.message}")
            }
            delay(retryDelay)
        }
        return null
    }

    private fun List<Place>.prepared(category: PlaceCategory): List<Place> =
        if (category != PlaceCategory.PARK) {
            this
        } else {
            filter { park -> park.outerRings.sumOf { it.areaInSquareMeters() } >= MIN_PARK_AREA_IN_SQUARE_METERS }
                .map { park ->
                    park.copy(
                        outerRings = park.outerRings.map { it.densified(OUTLINE_STEP_IN_METERS) },
                        innerRings = park.innerRings.map { it.densified(OUTLINE_STEP_IN_METERS) },
                    )
                }
        }
}

// Prague, where the portals are scraped, plus about 3 km so listings near the border see places just outside it
private val IMPORT_AREA_SOUTH_WEST = Coordinates(49.91, 14.18)
private val IMPORT_AREA_NORTH_EAST = Coordinates(50.21, 14.75)
// smaller green spaces are pocket squares, not somewhere to go for a walk
private const val MIN_PARK_AREA_IN_SQUARE_METERS = 5_000.0
private const val OUTLINE_STEP_IN_METERS = 25.0
