package cz.havasi.reality.app.rest.controller

import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.MarketStatistics
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.model.command.GetStatisticsCommand
import cz.havasi.reality.app.model.command.GroupByDimension
import cz.havasi.reality.app.model.command.TimeGranularity
import cz.havasi.reality.app.service.repository.ApartmentRepository
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC

@Path("/api/statistics")
@Produces(MediaType.APPLICATION_JSON)
public class StatisticsController(
    private val apartmentRepository: ApartmentRepository,
) {
    @GET
    @Path("/market")
    public suspend fun getMarketStatistics(
        @QueryParam("city") city: String?,
        @QueryParam("district") district: String?,
        @QueryParam("buildingType") buildingType: BuildingType?,
        @QueryParam("subCategory") subCategory: String?,
        @QueryParam("transactionType") transactionType: TransactionType?,
        @QueryParam("sizeMin") sizeMin: Double?,
        @QueryParam("sizeMax") sizeMax: Double?,
        @QueryParam("period") period: String = "6M",
        @QueryParam("granularity") granularity: TimeGranularity = TimeGranularity.MONTHLY,
        @QueryParam("groupBy") groupBy: List<GroupByDimension>?,
    ): List<MarketStatistics> {
        val (dateFrom, dateTo) = parsePeriod(period)

        val command = GetStatisticsCommand(
            city = city,
            district = district,
            buildingType = buildingType,
            subCategory = subCategory,
            transactionType = transactionType,
            sizeMin = sizeMin,
            sizeMax = sizeMax,
            dateFrom = dateFrom,
            dateTo = dateTo,
            granularity = granularity,
            groupBy = groupBy?.toSet() ?: emptySet(),
        )

        return apartmentRepository.getMarketStatistics(command)
    }

    private fun parsePeriod(period: String): Pair<OffsetDateTime, OffsetDateTime> {
        val now = OffsetDateTime.now(UTC)
        val dateFrom = when (period.uppercase()) {
            "1M" -> now.minusMonths(1)
            "3M" -> now.minusMonths(3)
            "6M" -> now.minusMonths(6)
            "1Y" -> now.minusYears(1)
            "2Y" -> now.minusYears(2)
            "5Y" -> now.minusYears(5)
            "MAX" -> OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, UTC)
            else -> now.minusMonths(6)
        }
        return Pair(dateFrom, now)
    }
}