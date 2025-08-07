package cz.havasi.reality.app.model.command

import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.model.util.Paging

public data class FindRealEstatesCommand(
    val searchString: String?,
    val transactionType: TransactionType,
    val buildingType: BuildingType,
    val sizeMin: Int,
    val sizeMax: Int,
    val priceMin: Int,
    val priceMax: Int,
    val paging: Paging,
)
