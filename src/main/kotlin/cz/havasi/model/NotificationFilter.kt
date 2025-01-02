package cz.havasi.model

public data class NotificationFilter(
    val buildingType: BuildingType,
    val transactionType: TransactionType,
    val size: FilterRange<Int>,
    val price: FilterRange<Double>,
)

public data class FilterRange<T>(
    val from: T,
    val to: T,
)
