package cz.havasi.model.command

import cz.havasi.model.BuildingType
import cz.havasi.model.TransactionType

public data class FindNotificationsForFilterCommand(
    val buildingType: BuildingType,
    val transactionType: TransactionType,
    val size: Double,
    val price: Int,
    val subTypes: List<String>,
)
