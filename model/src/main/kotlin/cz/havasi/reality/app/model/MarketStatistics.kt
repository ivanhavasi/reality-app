package cz.havasi.reality.app.model

import io.quarkus.runtime.annotations.RegisterForReflection

@RegisterForReflection
public data class MarketStatistics(
    val period: String, // "2026-01", "2026-02-15", "2026-W05"
    val grouping: Map<String, String?>, // e.g., {"district": "Praha 1", "buildingType": "APARTMENT"}
    val avgPrice: Double,
    val avgPricePerM2: Double?,
    val avgSize: Double,
    val minPrice: Double,
    val maxPrice: Double,
    val count: Long,
)