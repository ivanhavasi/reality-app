package cz.havasi.reality.app.repository.mongo

import cz.havasi.reality.app.AbstractIT
import cz.havasi.reality.app.helper.ApartmentHelper.APARTMENT
import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.model.command.GetStatisticsCommand
import cz.havasi.reality.app.model.command.GroupByDimension
import cz.havasi.reality.app.model.command.TimeGranularity
import io.quarkus.test.junit.QuarkusTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC

@QuarkusTest
internal class MongoClientApartmentRepositoryStatisticsIT : AbstractIT() {
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

    private val largePlot = APARTMENT.copy(
        id = "land-1",
        fingerprint = "land-Prague-SALE-25000",
        sizeInM2 = 25_000.0,
        price = 50_000_000.0,
        pricePerM2 = 2_000.0,
        mainCategory = BuildingType.LAND,
        subCategory = "BUILDING_PLOT",
    )

    private fun command(
        buildingType: BuildingType?,
        groupBy: Set<GroupByDimension> = emptySet(),
        subCategories: List<String> = emptyList(),
    ): GetStatisticsCommand {
        val now = OffsetDateTime.now(UTC)
        return GetStatisticsCommand(
            city = null,
            district = null,
            buildingType = buildingType,
            subCategories = subCategories,
            transactionType = TransactionType.SALE,
            sizeMin = null,
            sizeMax = null,
            dateFrom = now.minusDays(1),
            dateTo = now.plusMinutes(1),
            granularity = TimeGranularity.MONTHLY,
            groupBy = groupBy,
        )
    }

    @Test
    fun `statistics are computed for every building type`() = runTest {
        apartmentRepository.saveAll(listOf(APARTMENT, house, largePlot))

        val apartments = apartmentRepository.getMarketStatistics(command(BuildingType.APARTMENT)).single()
        assertEquals(1, apartments.count)
        assertEquals(62.0, apartments.avgSize)
        assertNull(apartments.avgLandSize)

        val houses = apartmentRepository.getMarketStatistics(command(BuildingType.HOUSE)).single()
        assertEquals(1, houses.count)
        assertEquals(342.0, houses.avgSize)
        assertEquals(769.0, houses.avgLandSize)
        assertEquals(69_854.0, houses.avgPricePerM2)

        val plots = apartmentRepository.getMarketStatistics(command(BuildingType.LAND)).single()
        assertEquals(1, plots.count)
        assertEquals(25_000.0, plots.avgSize)
    }

    @Test
    fun `statistics without a building type keep plots larger than any building`() = runTest {
        apartmentRepository.saveAll(listOf(APARTMENT, house, largePlot))

        val byType = apartmentRepository.getMarketStatistics(command(null, setOf(GroupByDimension.BUILDING_TYPE)))

        assertEquals(
            mapOf("APARTMENT" to 1L, "HOUSE" to 1L, "LAND" to 1L),
            byType.associate { it.grouping["buildingType"] to it.count },
        )
    }

    @Test
    fun `statistics combine the selected layouts`() = runTest {
        val twoKk = APARTMENT.copy(
            id = "2kk",
            fingerprint = "2kk",
            subCategory = "2+kk",
            sizeInM2 = 50.0,
            price = 6_000_000.0,
            pricePerM2 = 120_000.0,
        )
        val twoOne = APARTMENT.copy(
            id = "21",
            fingerprint = "21",
            subCategory = "2+1",
            sizeInM2 = 60.0,
            price = 6_000_000.0,
            pricePerM2 = 100_000.0,
        )
        apartmentRepository.saveAll(listOf(APARTMENT, twoKk, twoOne))

        val combined = apartmentRepository.getMarketStatistics(
            command(BuildingType.APARTMENT, subCategories = listOf("2+kk", "2+1")),
        ).single()

        assertEquals(2, combined.count)
        assertEquals(55.0, combined.avgSize)
        assertEquals(110_000.0, combined.avgPricePerM2)
    }
}
