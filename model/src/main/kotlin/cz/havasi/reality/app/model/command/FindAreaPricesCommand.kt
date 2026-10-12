package cz.havasi.reality.app.model.command

import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.Coordinates
import cz.havasi.reality.app.model.TransactionType
import java.time.OffsetDateTime

public data class FindAreaPricesCommand(
    val buildingType: BuildingType,
    val transactionType: TransactionType,
    val excludedId: String,
    val southWest: Coordinates,
    val northEast: Coordinates,
    val sizeMin: Double,
    val sizeMax: Double,
    val createdAfter: OffsetDateTime,
)
