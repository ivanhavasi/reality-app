package cz.havasi.reality.app.model

import io.quarkus.runtime.annotations.RegisterForReflection

@RegisterForReflection
public data class NotificationFilter(
    val buildingType: BuildingType,
    val transactionType: TransactionType,
    val size: FilterRange<Double>?,
    val price: FilterRange<Int>?,
    val subTypes: List<String>?,
)

@RegisterForReflection
public data class FilterRange<T : Number>(
    val from: T?,
    val to: T?,
)
