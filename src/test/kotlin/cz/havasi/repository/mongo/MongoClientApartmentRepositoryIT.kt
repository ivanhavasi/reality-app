package cz.havasi.repository.mongo

import cz.havasi.AbstractIT
import cz.havasi.helper.ApartmentHelper.APARTMENT
import cz.havasi.model.ApartmentDuplicate
import cz.havasi.model.command.UpdateApartmentWithDuplicateCommand
import cz.havasi.model.enum.ProviderType
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
        val id = repository.save(apartment)

        val findByBoth = repository.findByIdOrFingerprint(apartment.id, apartment.fingerprint)

        assertEquals(apartment, findByBoth) { "Apartment with id $id was not saved into mongodb." }
    }

    @Test
    fun `existsByIdOrFingerprint correctly finds already existing apartment`() = runTest {
        val existingApartment = APARTMENT
        repository.save(existingApartment)
        val existsByFingerprint = repository.existsByIdOrFingerprint("wrongId", existingApartment.fingerprint)
        val existsById = repository.existsByIdOrFingerprint(existingApartment.id, "wrongFingerprint")
        val existsByBoth = repository.existsByIdOrFingerprint(existingApartment.id, existingApartment.fingerprint)

        assertTrue(existsByFingerprint) { "Apartment not found by fingerprint" }
        assertTrue(existsById) { "Apartment not found by id" }
        assertTrue(existsByBoth) { "Apartment not found by fingerprint or id" }
    }

    @Test
    fun `findByIdOrFingerprint correctly finds already existing apartment`() = runTest {
        val existingApartment = APARTMENT
        repository.save(existingApartment)

        val findByFingerprint = repository.findByIdOrFingerprint("wrongId", existingApartment.fingerprint)
        val findById = repository.findByIdOrFingerprint(existingApartment.id, "wrongFingerprint")
        val findByBoth = repository.findByIdOrFingerprint(existingApartment.id, existingApartment.fingerprint)

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

        repository.save(existingApartment)
        repository.bulkUpdateApartmentWithDuplicate(listOf(UpdateApartmentWithDuplicateCommand(existingApartment, duplicate)))

        val found = repository.findByIdOrFingerprint(existingApartment.id, existingApartment.fingerprint)

        assertEquals(1, found?.duplicates?.size)
        assertEquals(duplicate, found?.duplicates?.get(0))
    }
}
