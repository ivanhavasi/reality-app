package cz.havasi.reality.app.model.command

import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.TransactionType

public data class GetRealEstatesCommand(
    val type: BuildingType,
    val transaction: TransactionType,
    val offset: Int,
    val limit: Int,
)
