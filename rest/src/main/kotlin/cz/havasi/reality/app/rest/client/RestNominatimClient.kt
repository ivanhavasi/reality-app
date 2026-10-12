package cz.havasi.reality.app.rest.client

import cz.havasi.reality.app.model.Coordinates
import cz.havasi.reality.app.model.StreetSegment
import cz.havasi.reality.app.rest.client.api.NominatimApi
import cz.havasi.reality.app.service.client.GeocodingClient
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.rest.client.inject.RestClient

@ApplicationScoped
internal class RestNominatimClient(
    @RestClient private val nominatimApi: NominatimApi,
) : GeocodingClient {
    override suspend fun findStreet(street: String, city: String): List<StreetSegment> =
        nominatimApi.searchStreet(
            street = street,
            city = city,
            country = "Česko",
            layer = "address",
            limit = MAX_SEGMENTS,
            format = "jsonv2",
            acceptLanguage = "cs",
        )
            .mapNotNull { place ->
                val latitude = place.latitude.toDoubleOrNull() ?: return@mapNotNull null
                val longitude = place.longitude.toDoubleOrNull() ?: return@mapNotNull null
                StreetSegment(Coordinates(latitude, longitude), place.displayName)
            }

    private companion object {
        // a long Prague street comes back as one result per city part it crosses
        const val MAX_SEGMENTS = 10
    }
}
