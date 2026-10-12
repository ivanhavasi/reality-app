package cz.havasi.reality.app.service.client

import cz.havasi.reality.app.model.StreetSegment

public interface GeocodingClient {
    // every known piece of the street in the city, empty when the street is unknown; throws when the service fails
    public suspend fun findStreet(street: String, city: String): List<StreetSegment>
}
