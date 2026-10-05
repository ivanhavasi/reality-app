package cz.havasi.reality.app.rest.util

import cz.havasi.reality.app.model.Apartment
import cz.havasi.reality.app.model.ApartmentDuplicate
import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.CurrencyType
import cz.havasi.reality.app.model.Locality
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.model.type.ProviderType

internal val LAND_PLOT = Apartment(
    id = "1140731980",
    fingerprint = "land-Praha-SALE-1200",
    name = "Prodej stavebního pozemku 1 200 m²",
    url = "https://www.sreality.cz/detail/prodej/pozemek/bydleni/praha-radotin-/1140731980",
    price = 3_200_000.0,
    pricePerM2 = 2_666.67,
    sizeInM2 = 1_200.0,
    currency = CurrencyType.CZK,
    locality = Locality(
        city = "Praha",
        district = "Radotín",
        street = null,
        streetNumber = null,
        latitude = null,
        longitude = null,
    ),
    mainCategory = BuildingType.LAND,
    subCategory = "BUILDING_PLOT",
    transactionType = TransactionType.SALE,
    description = null,
    provider = ProviderType.SREALITY,
)

internal val APARTMENT = LAND_PLOT.copy(
    id = "a1",
    fingerprint = "apartment-Praha-Havlíčkova-2+kk-SALE",
    name = "Prodej bytu 2+kk 65 m²",
    sizeInM2 = 65.0,
    price = 7_000_000.0,
    pricePerM2 = 107_692.0,
    locality = Locality("Praha", "Žižkov", "Havlíčkova", null, null, null),
    mainCategory = BuildingType.APARTMENT,
    subCategory = "2+kk",
)

internal fun duplicate(price: Double) =
    ApartmentDuplicate(url = "https://example.com", price = price, pricePerM2 = null, provider = ProviderType.IDNES, id = "d$price")

// the number formatter is locale dependent (a plain, no-break or narrow no-break space)
internal fun String.normalizeSpaces(): String = replace(' ', ' ').replace(' ', ' ')
