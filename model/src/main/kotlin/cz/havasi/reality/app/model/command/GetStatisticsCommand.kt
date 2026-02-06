package cz.havasi.reality.app.model.command

import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.TransactionType
import java.time.OffsetDateTime

public data class GetStatisticsCommand(
    val city: String?,
    val district: String?,
    val buildingType: BuildingType?,
    val subCategory: String?,
    val transactionType: TransactionType?,
    val sizeMin: Double?,
    val sizeMax: Double?,
    val dateFrom: OffsetDateTime?,
    val dateTo: OffsetDateTime?,
    val granularity: TimeGranularity = TimeGranularity.MONTHLY,
    val groupBy: Set<GroupByDimension> = emptySet(),
)

public enum class TimeGranularity {
    DAILY,
    WEEKLY,
    MONTHLY,
}

public enum class GroupByDimension {
    CITY,
    DISTRICT,
    STREET,
    BUILDING_TYPE,
    SUB_CATEGORY,
    TRANSACTION_TYPE,
}
