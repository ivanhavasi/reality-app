package cz.havasi.reality.app.service.repository

import cz.havasi.reality.app.model.StreetSegment

public interface GeocodingCacheRepository {
    // null when the street was never looked up; an empty list when it was and the geocoder didn't know it
    public suspend fun findStreet(key: String): List<StreetSegment>?
    public suspend fun saveStreet(key: String, segments: List<StreetSegment>)
}
