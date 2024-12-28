package cz.havasi.model

public data class GetEstatesCommand(
    val type: BuildingType,
    val transaction: TransactionType,
    val offset: Int,
    val limit: Int,
)
