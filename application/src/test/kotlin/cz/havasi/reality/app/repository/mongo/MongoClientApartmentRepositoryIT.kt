package cz.havasi.reality.app.repository.mongo

import cz.havasi.reality.app.AbstractIT
import cz.havasi.reality.app.helper.ApartmentHelper.APARTMENT
import cz.havasi.reality.app.model.ApartmentDuplicate
import cz.havasi.reality.app.model.command.FindRealEstatesCommand
import cz.havasi.reality.app.model.command.UpdateApartmentWithDuplicateCommand
import cz.havasi.reality.app.model.util.Paging
import cz.havasi.reality.app.model.type.ProviderType
import io.quarkus.test.junit.QuarkusTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

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
}
