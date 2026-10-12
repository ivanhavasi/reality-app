package cz.havasi.reality.app.repository.mongo

import cz.havasi.reality.app.AbstractIT
import cz.havasi.reality.app.helper.ApartmentHelper.APARTMENT
import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.model.command.FindRealEstatesCommand
import cz.havasi.reality.app.model.util.Paging
import io.quarkus.test.junit.QuarkusTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

@QuarkusTest
internal class MongoClientApartmentRepositoryHouseIT : AbstractIT() {
    private val house = APARTMENT.copy(
        id = "house-1",
        fingerprint = "house-Prague-SALE-342",
        sizeInM2 = 342.0,
        landSizeInM2 = 769.0,
        price = 23_890_000.0,
        pricePerM2 = 69_854.0,
        mainCategory = BuildingType.HOUSE,
        subCategory = "FAMILY_HOUSE",
    )

    private fun command(buildingType: BuildingType) = FindRealEstatesCommand(
        searchString = null,
        transactionType = TransactionType.SALE,
        buildingType = buildingType,
        sizeMin = 0,
        sizeMax = 1_000_000,
        priceMin = 0,
        priceMax = 1_000_000_000,
        paging = Paging(),
        subCategories = emptyList(),
    )

    @Test
    fun `house keeps its plot area and stays apart from apartments`() = runTest {
        apartmentRepository.saveAll(listOf(APARTMENT, house))

        val houses = apartmentRepository.findAll(command(BuildingType.HOUSE))
        assertEquals(listOf(house.id), houses.map { it.id })
        assertEquals(769.0, houses.single().landSizeInM2)
        assertNull(apartmentRepository.findAll(command(BuildingType.APARTMENT)).single().landSizeInM2)
    }

    @Test
    fun `findByIdOrFingerprint finds a house by its fingerprint`() = runTest {
        apartmentRepository.save(house)

        val found = apartmentRepository.findByIdOrFingerprint("i1", "house-Prague-SALE-342")

        assertEquals(listOf(house.id), found.map { it.id })
        assertEquals(769.0, found.single().landSizeInM2)
    }
}
