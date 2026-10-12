package cz.havasi.reality.app.service.client

import cz.havasi.reality.app.model.Coordinates
import cz.havasi.reality.app.model.Place
import cz.havasi.reality.app.model.PlaceCategory

public interface PlacesClient {
    // every place of the category inside the box; throws when the service fails
    public suspend fun findPlaces(category: PlaceCategory, southWest: Coordinates, northEast: Coordinates): List<Place>
}
