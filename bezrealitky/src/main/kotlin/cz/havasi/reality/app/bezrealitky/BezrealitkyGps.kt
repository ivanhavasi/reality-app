package cz.havasi.reality.app.bezrealitky

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import cz.havasi.reality.app.model.Apartment
import io.quarkus.logging.Log
import org.jsoup.nodes.Document

internal data class GpsPoint(val latitude: Double, val longitude: Double)

private val objectMapper = jacksonObjectMapper()

// the cards carry no coordinates, but Next.js embeds every advert of the page in __NEXT_DATA__
// (apolloCache "Advert:<id>" -> gps); a missing or changed payload only costs the coordinates, never the listings
internal fun Document.parseGpsByAdvertId(): Map<String, GpsPoint> =
    try {
        val json = selectFirst("script#__NEXT_DATA__")?.data()
        if (json.isNullOrBlank()) {
            emptyMap()
        } else {
            objectMapper.readTree(json).path("props").path("pageProps").path("apolloCache").properties()
                .filter { (key, _) -> key.startsWith("Advert:") }
                .mapNotNull { (_, advert) ->
                    val gps = advert.path("gps")
                    val latitude = gps.path("lat").takeIf { it.isNumber }?.doubleValue()
                    val longitude = gps.path("lng").takeIf { it.isNumber }?.doubleValue()
                    val id = advert.path("id").asText().ifBlank { null }
                    if (id == null || latitude == null || longitude == null) null else id to GpsPoint(latitude, longitude)
                }
                .toMap()
        }
    } catch (e: Exception) {
        Log.warn("Couldn't read Bezrealitky GPS data, listings stay without coordinates", e)
        emptyMap()
    }

// listing ids are the advert uri ("1077346-nabidka-prodej-bytu-..."), the cache is keyed by the numeric part
internal fun Apartment.withGps(gpsByAdvertId: Map<String, GpsPoint>): Apartment =
    gpsByAdvertId[id.substringBefore('-')]
        ?.let { copy(locality = locality.copy(latitude = it.latitude, longitude = it.longitude)) }
        ?: this
