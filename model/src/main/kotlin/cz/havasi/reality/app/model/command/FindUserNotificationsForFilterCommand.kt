package cz.havasi.reality.app.model.command

import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.TransactionType

public data class FindUserNotificationsForFilterCommand(
    val buildingType: BuildingType,
    val transactionType: TransactionType,
    val size: Double,
    val price: Int,
    val subTypes: List<String>,
)
