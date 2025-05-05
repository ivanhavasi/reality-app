package cz.havasi.reality.app.helper

import cz.havasi.reality.app.model.*
import cz.havasi.reality.app.mongo.entity.ApartmentEntity
import cz.havasi.reality.app.mongo.entity.LocalityEntity
import org.bson.types.ObjectId
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC

object ApartmentHelper {
    private val time = OffsetDateTime.of(2023, 10, 1, 0, 0, 0, 0, UTC)
    internal val APARTMENT_ENTITY = ApartmentEntity(
        _id = ObjectId("65b9a3f5e1b2a3456789abcd"),
        externalId = "123",
        fingerprint = "12345678910",
        locality = LocalityEntity(
            city = "Prague",
            district = "Zizkov",
            street = "Havlickova",
            streetNumber = "10",
            latitude = 43.9,
            longitude = 45.6,
        ),
        price = 10000000.0,
        name = "Beautiful apartment in Prague",
        url = "https://example.com/apartment/123",
        pricePerM2 = 120000.0,
        sizeInM2 = 62.0,
        currency = "CZK",
        mainCategory = "APARTMENT",
        subCategory = "3+kk",
        transactionType = "SALE",
        images = listOf(
            "https://example.com/image1.jpg",
        ),
        description = "Beautiful apartment in the heart of Prague",
        duplicates = listOf(),
        createdAt = time,
        updatedAt = time,
    )

    internal val APARTMENT = Apartment(
        id = "65b9a3f5e1b2a3456789abcd",
        fingerprint = "12345678910",
        locality = Locality(
            city = "Prague",
            district = "Zizkov",
            street = "Havlickova",
            streetNumber = "10",
            latitude = 43.9,
            longitude = 45.6,
        ),
        price = 10000000.0,
        name = "Beautiful apartment in Prague",
        url = "https://example.com/apartment/123",
        pricePerM2 = 120000.0,
        sizeInM2 = 62.0,
        currency = CurrencyType.CZK,
        mainCategory = BuildingType.APARTMENT,
        subCategory = "3+kk",
        transactionType = TransactionType.SALE,
        images = listOf(
            "https://example.com/image1.jpg",
        ),
        description = "Beautiful apartment in the heart of Prague",
        duplicates = listOf(),
    )
}