package cz.havasi.reality.app.idnes

import cz.havasi.reality.app.model.Apartment
import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.CurrencyType
import cz.havasi.reality.app.model.command.GetRealEstatesCommand
import cz.havasi.reality.app.model.type.HouseSubCategory
import cz.havasi.reality.app.model.type.ProviderType
import cz.havasi.reality.app.service.util.constructFingerprint
import cz.havasi.reality.app.service.util.firstCapitalOthersLowerCase
import cz.havasi.reality.app.service.util.plotSizeOrNull
import io.quarkus.logging.Log
import org.jsoup.nodes.Element

// a plain "domu" doesn't tell a family house from anything else
private val HOUSE_TITLE_NOUNS = mapOf(
    "vily" to HouseSubCategory.VILLA,
    "chaty/chalupy" to HouseSubCategory.COTTAGE,
)

// "Prodej chaty/chalupy 21 m²" -> COTTAGE
internal fun String.parseHouseSubCategory(): HouseSubCategory? =
    lowercase().split(" ").drop(1)
        .takeWhile { word -> word.firstOrNull()?.isLetter() == true }
        .joinToString(" ")
        .let { HOUSE_TITLE_NOUNS[it] }

// "Prodej domu 252 m² s pozemkem 1 774 m²": usable area first, the plot is optional
internal fun Element.toHouse(command: GetRealEstatesCommand): Apartment? {
    val linkElement = selectFirst("a.c-products__link") ?: return null
    val url = linkElement.attr("href")
    val id = url.split("/").let { it.getOrNull(it.lastIndex - 1) }?.ifBlank { null }
        ?: return null.also { Log.warn("iDnes house card has no id in '$url', skipping") }
    val name = selectFirst(".c-products__title")?.text()?.firstCapitalOthersLowerCase()
        ?: return null.also { Log.warn("iDnes house $id has no title, skipping") }
    val size = name.parseSize() ?: return null.also { Log.warn("iDnes house $id has no area in '$name', skipping") }
    val locality = selectFirst(".c-products__info")?.text()?.toCityPartLocality()
        ?: return null.also { Log.warn("iDnes house $id has no locality, skipping") }
    val price = getPrice(command.transaction)
    return Apartment(
        id = id,
        fingerprint = constructFingerprint(BuildingType.HOUSE, locality, "", command.transaction, size),
        name = name,
        url = url,
        price = price,
        pricePerM2 = price / size,
        sizeInM2 = size,
        landSizeInM2 = name.substringAfter("pozemkem", "").parseSize()?.plotSizeOrNull(),
        currency = CurrencyType.CZK,
        locality = locality,
        mainCategory = BuildingType.HOUSE,
        subCategory = name.parseHouseSubCategory()?.name,
        transactionType = command.transaction,
        images = listOfNotNull(selectFirst(".c-products__img img")?.attr("data-src")?.ifBlank { null }),
        provider = ProviderType.IDNES,
    )
}
