package cz.havasi.reality.app.repository.mongo

import cz.havasi.reality.app.AbstractIT
import cz.havasi.reality.app.helper.ApartmentHelper.APARTMENT
import cz.havasi.reality.app.helper.ApartmentHelper.APARTMENT_ENTITY
import cz.havasi.reality.app.model.Apartment
import cz.havasi.reality.app.model.ApartmentDuplicate
import cz.havasi.reality.app.model.AreaPrice
import cz.havasi.reality.app.model.Coordinates
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.model.command.FindAreaPricesCommand
import cz.havasi.reality.app.model.command.FindRealEstatesCommand
import cz.havasi.reality.app.model.command.UpdateApartmentWithDuplicateCommand
import cz.havasi.reality.app.model.util.Paging
import cz.havasi.reality.app.model.type.ProviderType
import io.quarkus.test.junit.QuarkusTest
import io.smallrye.mutiny.coroutines.awaitSuspending
import kotlinx.coroutines.test.runTest
import org.bson.types.ObjectId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC

@QuarkusTest
internal class MongoClientApartmentRepositoryIT : AbstractIT() {
    @Test
    fun `save saves apartment into mongodb`() = runTest {
        val apartment = APARTMENT
        val id = apartmentRepository.save(apartment)

        val findByBoth = apartmentRepository.findByIdOrFingerprint(apartment.id, apartment.fingerprint).firstOrNull()

        assertEquals(apartment, findByBoth) { "Apartment with id $id was not saved into mongodb." }
    }

    @Test
    fun `existsByIdOrFingerprint correctly finds already existing apartment`() = runTest {
        val existingApartment = APARTMENT
        apartmentRepository.save(existingApartment)
        val existsByFingerprint = apartmentRepository.existsByIdOrFingerprint("wrongId", existingApartment.fingerprint)
        val existsById = apartmentRepository.existsByIdOrFingerprint(existingApartment.id, "wrongFingerprint")
        val existsByBoth = apartmentRepository.existsByIdOrFingerprint(existingApartment.id, existingApartment.fingerprint)

        assertTrue(existsByFingerprint) { "Apartment not found by fingerprint" }
        assertTrue(existsById) { "Apartment not found by id" }
        assertTrue(existsByBoth) { "Apartment not found by fingerprint or id" }
    }

    @Test
    fun `findByIdOrFingerprint correctly finds already existing apartment`() = runTest {
        val existingApartment = APARTMENT
        apartmentRepository.save(existingApartment)

        val findByFingerprint = apartmentRepository.findByIdOrFingerprint("wrongId", existingApartment.fingerprint).firstOrNull()
        val findById = apartmentRepository.findByIdOrFingerprint(existingApartment.id, "wrongFingerprint").firstOrNull()
        val findByBoth =
            apartmentRepository.findByIdOrFingerprint(existingApartment.id, existingApartment.fingerprint).firstOrNull()

        assertTrue(findByFingerprint?.id == existingApartment.id) { "Apartment not found by fingerprint" }
        assertTrue(findById?.id == existingApartment.id) { "Apartment not found by id" }
        assertTrue(findByBoth?.id == existingApartment.id) { "Apartment not found by fingerprint or id" }
    }

    @Test
    fun `updateAll correctly updates existing apartment`() = runTest {
        val existingApartment = APARTMENT
        val duplicate = ApartmentDuplicate(
            url = "test",
            price = 10.0,
            pricePerM2 = 1.0,
            images = emptyList(),
            provider = ProviderType.SREALITY,
        )

        apartmentRepository.save(existingApartment)
        apartmentRepository.bulkUpdateApartmentWithDuplicate(
            listOf(
                UpdateApartmentWithDuplicateCommand(
                    existingApartment,
                    duplicate,
                ),
            ),
        )

        val found = apartmentRepository.findByIdOrFingerprint(existingApartment.id, existingApartment.fingerprint).firstOrNull()

        assertEquals(1, found?.duplicates?.size)
        assertEquals(duplicate, found?.duplicates?.get(0))
    }

    @Test
    fun `findAll withCoordinates skips listings without coordinates`() = runTest {
        val withoutCoordinates = APARTMENT.copy(
            id = "65b9a3f5e1b2a3456789abce",
            fingerprint = "no-coordinates",
            locality = APARTMENT.locality.copy(latitude = null, longitude = null),
        )
        apartmentRepository.save(APARTMENT)
        apartmentRepository.save(withoutCoordinates)
        val command = FindRealEstatesCommand(
            searchString = null,
            transactionType = APARTMENT.transactionType,
            buildingType = APARTMENT.mainCategory,
            sizeMin = 0,
            sizeMax = 1_000_000,
            priceMin = 0,
            priceMax = 1_000_000_000,
            paging = Paging(),
        )

        val all = apartmentRepository.findAll(command)
        val mappable = apartmentRepository.findAll(command.copy(withCoordinates = true))

        assertEquals(2, all.size)
        assertEquals(listOf(APARTMENT.id), mappable.map { it.id })
    }

    @Test
    fun `findWithoutCoordinates returns listings with a street but no coordinates`() = runTest {
        val unlocated = APARTMENT.copy(
            id = "65b9a3f5e1b2a3456789abcf",
            fingerprint = "unlocated",
            locality = APARTMENT.locality.copy(latitude = null, longitude = null),
        )
        val withoutStreet = unlocated.copy(
            id = "65b9a3f5e1b2a3456789abd0",
            fingerprint = "without-street",
            locality = unlocated.locality.copy(street = null),
        )
        apartmentRepository.save(APARTMENT)
        apartmentRepository.save(unlocated)
        apartmentRepository.save(withoutStreet)

        val found = apartmentRepository.findWithoutCoordinates(APARTMENT.mainCategory, APARTMENT.transactionType)

        assertEquals(listOf(unlocated.id), found.map { it.id })
    }

    @Test
    fun `bulkUpdateLocations fills missing coordinates and keeps existing ones`() = runTest {
        val withoutCoordinates = APARTMENT.copy(
            id = "65b9a3f5e1b2a3456789abce",
            fingerprint = "no-coordinates",
            locality = APARTMENT.locality.copy(latitude = null, longitude = null),
        )
        apartmentRepository.save(APARTMENT)
        apartmentRepository.save(withoutCoordinates)
        val moved = { apartment: Apartment -> apartment.copy(locality = apartment.locality.copy(latitude = 50.08, longitude = 14.44)) }

        val filled = apartmentRepository.bulkUpdateLocations(listOf(moved(APARTMENT), moved(withoutCoordinates)))

        assertEquals(1, filled)

        suspend fun localityOf(id: String) = apartmentRepository.findByIdOrFingerprint(id, "").first { it.id == id }.locality
        assertEquals(APARTMENT.locality.latitude, localityOf(APARTMENT.id).latitude)
        assertEquals(APARTMENT.locality.longitude, localityOf(APARTMENT.id).longitude)
        assertEquals(50.08, localityOf(withoutCoordinates.id).latitude)
        assertEquals(14.44, localityOf(withoutCoordinates.id).longitude)
    }

    @Test
    fun `findAreaPrices returns recent similar listings in the box at their lowest price`() = runTest {
        val near = APARTMENT.copy(
            id = "area-near",
            fingerprint = "area-near",
            locality = APARTMENT.locality.copy(latitude = 50.08, longitude = 14.44),
            pricePerM2 = 130_000.0,
            duplicates = listOf(
                ApartmentDuplicate(url = "https://example.com/cheaper", price = 7_440_000.0, pricePerM2 = 120_000.0, provider = ProviderType.BEZREALITKY),
                ApartmentDuplicate(url = "https://example.com/on-request", price = 0.0, pricePerM2 = 0.0, provider = ProviderType.IDNES),
            ),
        )
        val located = { id: String, latitude: Double, longitude: Double ->
            near.copy(id = id, fingerprint = id, duplicates = emptyList(), locality = near.locality.copy(latitude = latitude, longitude = longitude))
        }
        apartmentRepository.save(near)
        apartmentRepository.save(near.copy(id = "area-subject", fingerprint = "area-subject"))
        apartmentRepository.save(located("area-north", 50.2, 14.44))
        apartmentRepository.save(located("area-east", 50.08, 14.6))
        apartmentRepository.save(located("area-rent", 50.08, 14.44).copy(transactionType = TransactionType.RENT))
        apartmentRepository.save(located("area-large", 50.08, 14.44).copy(sizeInM2 = 200.0))
        apartmentRepository.save(located("area-unpriced", 50.08, 14.44).copy(pricePerM2 = null))
        apartmentCollection.insertOne(
            APARTMENT_ENTITY.copy(
                _id = ObjectId.get(),
                externalId = "area-old",
                fingerprint = "area-old",
                locality = APARTMENT_ENTITY.locality.copy(latitude = 50.08, longitude = 14.44),
            ),
        ).awaitSuspending()

        val found = apartmentRepository.findAreaPrices(
            FindAreaPricesCommand(
                buildingType = APARTMENT.mainCategory,
                transactionType = TransactionType.SALE,
                excludedId = "area-subject",
                southWest = Coordinates(50.07, 14.43),
                northEast = Coordinates(50.09, 14.45),
                sizeMin = 40.0,
                sizeMax = 90.0,
                createdAfter = OffsetDateTime.now(UTC).minusMonths(12),
            ),
        )

        assertEquals(listOf(AreaPrice(Coordinates(50.08, 14.44), 120_000.0)), found)
    }
}
