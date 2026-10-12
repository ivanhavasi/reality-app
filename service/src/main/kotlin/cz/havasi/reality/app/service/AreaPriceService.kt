package cz.havasi.reality.app.service

import cz.havasi.reality.app.model.Apartment
import cz.havasi.reality.app.model.AreaPriceComparison
import cz.havasi.reality.app.model.Coordinates
import cz.havasi.reality.app.model.command.FindAreaPricesCommand
import cz.havasi.reality.app.service.repository.ApartmentRepository
import cz.havasi.reality.app.service.util.boundingBox
import cz.havasi.reality.app.service.util.distanceInMetersTo
import jakarta.enterprise.context.ApplicationScoped
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC
import kotlin.math.roundToInt

@ApplicationScoped
public class AreaPriceService(
    private val apartmentRepository: ApartmentRepository,
) {
    // compared with listings of the same type, transaction and similar size, from the smallest radius that has enough of them
    public suspend fun compare(listing: Apartment, now: OffsetDateTime = OffsetDateTime.now(UTC)): AreaPriceComparison? {
        val latitude = listing.locality.latitude ?: return null
        val longitude = listing.locality.longitude ?: return null
        val pricePerM2 = listing.lowestPricePerM2() ?: return null
        if (listing.sizeInM2 <= 0) return null

        val center = Coordinates(latitude, longitude)
        val (southWest, northEast) = center.boundingBox(RADII_IN_METERS.last())
        val nearby = apartmentRepository.findAreaPrices(
            FindAreaPricesCommand(
                buildingType = listing.mainCategory,
                transactionType = listing.transactionType,
                excludedId = listing.id,
                southWest = southWest,
                northEast = northEast,
                sizeMin = listing.sizeInM2 / SIMILAR_SIZE_FACTOR,
                sizeMax = listing.sizeInM2 * SIMILAR_SIZE_FACTOR,
                createdAfter = now.minusMonths(COMPARABLE_MONTHS),
            ),
        ).map { center.distanceInMetersTo(it.coordinates) to it.pricePerM2 }

        val (radius, prices) = RADII_IN_METERS
            .map { radius -> radius to nearby.filter { (distance, _) -> distance <= radius }.map { it.second } }
            .firstOrNull { (_, prices) -> prices.size >= MIN_COMPARABLES }
            ?: return null
        val areaPricePerM2 = prices.median()
        val ratio = pricePerM2 / areaPricePerM2
        // that far off is a placeholder price or a wrong floor area on the portal, not a bargain
        if (ratio < 1 / OUTLIER_FACTOR || ratio > OUTLIER_FACTOR) return null

        return AreaPriceComparison(
            pricePerM2 = pricePerM2,
            areaPricePerM2 = areaPricePerM2,
            differencePercent = ((ratio - 1) * 100).roundToInt(),
            comparableCount = prices.size,
            radiusInMeters = radius,
        )
    }

    private fun Apartment.lowestPricePerM2(): Double? =
        (listOf(pricePerM2) + duplicates.map { it.pricePerM2 }).filterNotNull().filter { it > 0 }.minOrNull()

    private fun List<Double>.median(): Double {
        val sorted = sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2
    }
}

private val RADII_IN_METERS = listOf(500, 1_000, 2_000)
private const val MIN_COMPARABLES = 10
// per-m² prices of studios and family flats differ, so only sizes within this factor count as similar
private const val SIMILAR_SIZE_FACTOR = 1.5
private const val COMPARABLE_MONTHS = 12L
private const val OUTLIER_FACTOR = 3.0
