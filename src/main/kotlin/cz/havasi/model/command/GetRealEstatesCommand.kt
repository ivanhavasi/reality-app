package cz.havasi.model.command

import cz.havasi.model.BuildingType
import cz.havasi.model.TransactionType

public data class GetRealEstatesCommand(
    val type: BuildingType,
    val transaction: TransactionType,
    val offset: Int,
    val limit: Int,
)
