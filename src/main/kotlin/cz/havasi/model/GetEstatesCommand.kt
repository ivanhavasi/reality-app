package cz.havasi.model

public data class GetEstatesCommand(
    val type: BuildingType,
    val transaction: TransactionType,
)
