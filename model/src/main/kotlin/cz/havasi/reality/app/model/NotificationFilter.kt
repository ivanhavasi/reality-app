package cz.havasi.reality.app.model

public data class NotificationFilter(
    val buildingType: BuildingType,
    val transactionType: TransactionType,
    val size: FilterRange<Double>?,
    val price: FilterRange<Int>?,
    val subTypes: List<String>?,
)

public data class FilterRange<T : Number>(
    val from: T?,
    val to: T?,
)
