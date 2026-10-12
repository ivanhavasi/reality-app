package cz.havasi.reality.app.bezrealitky

import cz.havasi.reality.app.model.Apartment
import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.CurrencyType
import cz.havasi.reality.app.model.command.GetRealEstatesCommand
import cz.havasi.reality.app.model.type.ProviderType
import cz.havasi.reality.app.service.util.constructFingerprint
import cz.havasi.reality.app.service.util.plotSizeOrNull
import io.quarkus.logging.Log
import org.jsoup.nodes.Element

// features are "disposition, usable area, plot area"; the disposition isn't a house type, so the subtype stays empty
internal fun Element.toHouse(command: GetRealEstatesCommand): Apartment? {
    val headline = selectFirst(".PropertyCard_propertyCardHeadline___diKI")
    val url = headline?.selectFirst("a")?.attr("href")?.ifBlank { null }
        ?: return null.also { Log.warn("Bezrealitky house card has no url, skipping") }
    val id = url.substringAfter("/nemovitosti-byty-domy/")
    val areas = getElementsByClass("FeaturesList_featuresList__75Wet").select("li")
        .map { it.text().trim() }
        .filter { it.endsWith("m²") }
        .mapNotNull { it.parseSquareMeters() }
    val size = areas.firstOrNull() ?: return null.also { Log.warn("Bezrealitky house $id has no area, skipping") }
    val price = selectFirst(".PropertyPrice_propertyPriceAmount__WdEE1")?.text()?.substringBefore("Kč")
        ?.filter { it.isDigit() }?.toDoubleOrNull() ?: 0.0
    val locality = selectFirst(".PropertyCard_propertyCardAddress__hNqyR")?.text()?.toCityPartLocality()
        ?: return null.also { Log.warn("Bezrealitky house $id has no address, skipping") }
    return Apartment(
        id = id,
        fingerprint = constructFingerprint(BuildingType.HOUSE, locality, "", command.transaction, size),
        name = headline.headlineText().ifBlank { id },
        url = url,
        price = price,
        pricePerM2 = price / size,
        sizeInM2 = size,
        landSizeInM2 = areas.getOrNull(1)?.plotSizeOrNull(),
        currency = CurrencyType.CZK,
        locality = locality,
        mainCategory = BuildingType.HOUSE,
        subCategory = null,
        transactionType = command.transaction,
        images = parseImages(),
        provider = ProviderType.BEZREALITKY,
    )
}
