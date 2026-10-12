package cz.havasi.reality.app.service

import cz.havasi.reality.app.model.Apartment
import cz.havasi.reality.app.model.Coordinates
import cz.havasi.reality.app.model.Locality
import cz.havasi.reality.app.model.StreetSegment
import cz.havasi.reality.app.service.client.GeocodingClient
import cz.havasi.reality.app.service.repository.GeocodingCacheRepository
import cz.havasi.reality.app.service.util.toComparableCityPart
import cz.havasi.reality.app.service.util.toComparableText
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// street-level coordinates from the address, for listings whose portal sends none (iDNES). The geocoder is
// OpenStreetMap's Nominatim, which allows one request per second: lookups run one at a time, and every answer,
// "unknown street" included, is cached per street, so each street is asked about once
@ApplicationScoped
public class AddressLocator(
    private val geocodingClient: GeocodingClient,
    private val cache: GeocodingCacheRepository,
) {
    private val oneAtATime = Mutex()

    public suspend fun withLocatedAddresses(apartments: List<Apartment>): List<Apartment> =
        apartments.map { apartment ->
            if (apartment.locality.latitude != null && apartment.locality.longitude != null) {
                apartment
            } else {
                locate(apartment.locality)
                    ?.let { apartment.copy(locality = apartment.locality.copy(latitude = it.latitude, longitude = it.longitude)) }
                    ?: apartment
            }
        }

    public suspend fun locate(locality: Locality): Coordinates? {
        val street = locality.street?.trim()?.takeIf { it.isStreetName() } ?: return null
        val city = locality.city.trim().takeIf { it.isNotBlank() && !it.startsWith(UNKNOWN) } ?: return null
        return segmentsOf(street, city)?.pickFor(locality.district)
    }

    private suspend fun segmentsOf(street: String, city: String): List<StreetSegment>? {
        val key = "${street.toComparableText()}|${city.toComparableText()}"
        cache.findStreet(key)?.let { return it }

        return oneAtATime.withLock {
            // another listing on the same street may have looked it up while this one waited
            cache.findStreet(key)?.let { return@withLock it }
            try {
                geocodingClient.findStreet(street, city).also { cache.saveStreet(key, it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // not cached, so the street is tried again next time
                Log.warn("Geocoding $street, $city failed: ${e.message}")
                null
            } finally {
                delay(MIN_REQUEST_INTERVAL_MS)
            }
        }
    }

    // a long street crosses several city parts: take the piece in the listing's part, otherwise the street's middle
    private fun List<StreetSegment>.pickFor(district: String?): Coordinates? {
        if (isEmpty()) return null
        val cityPart = district?.takeUnless { it.startsWith(UNKNOWN) }.toComparableCityPart()
        val inCityPart = cityPart?.let { part ->
            firstOrNull { segment -> segment.description.split(",").mapNotNull { it.toComparableText() }.contains(part) }
        }
        return inCityPart?.coordinates
            ?: Coordinates(map { it.coordinates.latitude }.average(), map { it.coordinates.longitude }.average())
    }

    // without a street, the first part of a portal's address is the city part ("Praha 4 - Krč") or a placeholder
    private fun String.isStreetName() = length > 1 && !startsWith(UNKNOWN) && !startsWith("Praha")

    private companion object {
        // scrapers store "Unknown <field>" when a card lacks it
        const val UNKNOWN = "Unknown "
        const val MIN_REQUEST_INTERVAL_MS = 1_100L
    }
}
