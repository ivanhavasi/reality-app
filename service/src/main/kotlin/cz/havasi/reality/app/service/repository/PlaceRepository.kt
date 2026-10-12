package cz.havasi.reality.app.service.repository

import cz.havasi.reality.app.model.Coordinates
import cz.havasi.reality.app.model.NearbyPlace
import cz.havasi.reality.app.model.Place
import cz.havasi.reality.app.model.PlaceCategory

public interface PlaceRepository {
    // the category's places afterwards are exactly these
    public suspend fun replaceCategory(category: PlaceCategory, places: List<Place>)
    public suspend fun findNearest(category: PlaceCategory, coordinates: Coordinates, maxDistanceInMeters: Int): NearbyPlace?
}
