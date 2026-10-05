package cz.havasi.reality.app.repository.mongo

import cz.havasi.reality.app.AbstractIT
import cz.havasi.reality.app.helper.ApartmentHelper.APARTMENT
import cz.havasi.reality.app.model.ApartmentDuplicate
import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.model.command.FindRealEstatesCommand
import cz.havasi.reality.app.model.command.UpdateApartmentWithDuplicateCommand
import cz.havasi.reality.app.model.type.ProviderType
import cz.havasi.reality.app.model.util.Paging
import io.quarkus.test.junit.QuarkusTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@QuarkusTest
internal class MongoClientApartmentRepositoryLandIT : AbstractIT() {
    private val plot = APARTMENT.copy(
        id = "land-1",
        fingerprint = "land-Prague-SALE-1418",
        sizeInM2 = 1_418.0,
        price = 14_000_000.0,
        pricePerM2 = 9_873.0,
        mainCategory = BuildingType.LAND,
        subCategory = "BUILDING_PLOT",
    )

    private fun command(
        buildingType: BuildingType,
        subCategories: List<String> = emptyList(),
        sizeMax: Int = 1_000_000,
    ) = FindRealEstatesCommand(
        searchString = null,
        transactionType = TransactionType.SALE,
        buildingType = buildingType,
        sizeMin = 0,
        sizeMax = sizeMax,
        priceMin = 0,
        priceMax = 1_000_000_000,
        paging = Paging(),
        subCategories = subCategories,
    )

    @Test
    fun `findAll keeps land and apartments apart`() = runTest {
        apartmentRepository.saveAll(listOf(APARTMENT, plot))

        assertEquals(listOf(plot.id), apartmentRepository.findAll(command(BuildingType.LAND)).map { it.id })
        assertEquals(listOf(APARTMENT.id), apartmentRepository.findAll(command(BuildingType.APARTMENT)).map { it.id })
    }

    @Test
    fun `findAll filters by subCategories`() = runTest {
        val garden = plot.copy(id = "land-2", fingerprint = "land-Prague-SALE-500", subCategory = "GARDEN")
        apartmentRepository.saveAll(listOf(plot, garden))

        assertEquals(listOf(plot.id), apartmentRepository.findAll(command(BuildingType.LAND, listOf("BUILDING_PLOT"))).map { it.id })
        assertEquals(2, apartmentRepository.findAll(command(BuildingType.LAND)).size)
    }

    @Test
    fun `findAll returns plots over 1000 m2 under the new default`() = runTest {
        apartmentRepository.save(plot)

        assertEquals(listOf(plot.id), apartmentRepository.findAll(command(BuildingType.LAND)).map { it.id })
        assertEquals(emptyList<String>(), apartmentRepository.findAll(command(BuildingType.LAND, sizeMax = 1_000)).map { it.id })
    }

    @Test
    fun `findByIdOrFingerprint finds an original by its duplicate id`() = runTest {
        val duplicate = ApartmentDuplicate(
            url = "https://example.com/b1",
            price = 13_900_000.0,
            pricePerM2 = null,
            provider = ProviderType.BEZREALITKY,
            id = "b1",
        )
        apartmentRepository.save(plot)
        apartmentRepository.bulkUpdateApartmentWithDuplicate(listOf(UpdateApartmentWithDuplicateCommand(plot, duplicate)))

        val found = apartmentRepository.findByIdOrFingerprint("b1", "land-Prague-SALE-9999")

        assertEquals(listOf(plot.id), found.map { it.id })
        assertEquals(listOf(duplicate), found.single().duplicates)
    }
}
