package cz.havasi.reality.app.bezrealitky

import cz.havasi.reality.app.model.Apartment
import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.CurrencyType
import cz.havasi.reality.app.model.Locality
import cz.havasi.reality.app.model.command.GetRealEstatesCommand
import cz.havasi.reality.app.model.type.LandSubCategory
import cz.havasi.reality.app.model.type.ProviderType
import cz.havasi.reality.app.service.util.constructFingerprint
import cz.havasi.reality.app.service.util.looksLikePricePerM2
import io.quarkus.logging.Log
import org.jsoup.nodes.Element
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

internal fun GetRealEstatesCommand.prepareLandType(): String? =
    if (type != BuildingType.LAND) {
        null
    } else {
        when (landSubCategory) {
            LandSubCategory.BUILDING_PLOT -> "STAVEBNI"
            LandSubCategory.COMMERCIAL -> "KOMERCNI"
            LandSubCategory.FIELD -> "POLE"
            LandSubCategory.MEADOW -> "LOUKA"
            LandSubCategory.FOREST -> "LES"
            LandSubCategory.GARDEN -> "ZAHRADA"
            LandSubCategory.POND -> "RYBNIK"
            LandSubCategory.OTHER -> "OSTATNI"
            LandSubCategory.ORCHARD_VINEYARD, null -> null
        }
    }

// results never carry the land type, so the subtype comes from the request
internal fun Element.toLand(command: GetRealEstatesCommand): Apartment? {
    val headline = selectFirst(".PropertyCard_propertyCardHeadline___diKI")
    val url = headline?.selectFirst("a")?.attr("href")?.ifBlank { null }
        ?: return null.also { Log.warn("Bezrealitky land card has no url, skipping") }
    val id = url.substringAfter("/nemovitosti-byty-domy/")
    val size = getElementsByClass("FeaturesList_featuresList__75Wet").select("li")
        .map { it.text().trim() }
        .firstOrNull { it.endsWith("m²") }
        ?.parseSquareMeters()
        ?: return null.also { Log.warn("Bezrealitky land $id has no area, skipping") }
    val price = selectFirst(".PropertyPrice_propertyPriceAmount__WdEE1")?.text()?.substringBefore("Kč")
        ?.filter { it.isDigit() }?.toDoubleOrNull() ?: 0.0
    if (command.landSubCategory == LandSubCategory.BUILDING_PLOT && looksLikePricePerM2(price, size)) {
        Log.warn("Bezrealitky land $id: $price Kč for $size m² looks like a per-m² price, skipping")
        return null
    }
    val locality = selectFirst(".PropertyCard_propertyCardAddress__hNqyR")?.text()?.toLandLocality()
        ?: return null.also { Log.warn("Bezrealitky land $id has no address, skipping") }
    return Apartment(
        id = id,
        fingerprint = constructFingerprint(
            BuildingType.LAND,
            locality,
            command.landSubCategory?.name ?: "",
            command.transaction,
            size,
        ),
        name = headline.headlineText().ifBlank { id },
        url = url,
        price = price,
        pricePerM2 = price / size,
        sizeInM2 = size,
        currency = CurrencyType.CZK,
        locality = locality,
        mainCategory = BuildingType.LAND,
        subCategory = command.landSubCategory?.name,
        transactionType = command.transaction,
        images = parseImages(),
        provider = ProviderType.BEZREALITKY,
    )
}

internal fun Element.parseImages(): List<String> =
    select("img")
        .map { it.attr("src").substringAfter("/_next/image?url=") }
        .map { URLDecoder.decode(it, StandardCharsets.UTF_8) }
        .map { it.substringBefore("&w=") }

private fun String.parseSquareMeters(): Double? =
    substringBefore("m²").filter { it.isDigit() || it == ',' }.replace(',', '.').toDoubleOrNull()

// Prague: "[Street, ]Praha - Part"
private fun String.toLandLocality(): Locality {
    val parts = split(",").map { it.trim() }.filter { it.isNotEmpty() }
    return Locality(
        city = "Praha",
        district = parts.lastOrNull()?.substringAfter(" - ", "")?.ifBlank { null },
        street = parts.takeIf { it.size > 1 }?.first(),
        streetNumber = null,
        latitude = null,
        longitude = null,
    )
}

// the label ("Prodej pozemku") and the address are adjacent spans, so text() would glue them together
private fun Element.headlineText(): String =
    selectFirst("a")?.children()?.takeIf { it.isNotEmpty() }?.joinToString(" ") { it.text() } ?: text()
