package cz.havasi.reality.app.model

import io.quarkus.runtime.annotations.RegisterForReflection

// a listing's price per m² against similar listings around it; prices are the lowest across portals
@RegisterForReflection
public data class AreaPriceComparison(
    val pricePerM2: Double,
    // median, so a few mispriced listings don't move it
    val areaPricePerM2: Double,
    val differencePercent: Int,
    val comparableCount: Int,
    val radiusInMeters: Int,
)

@RegisterForReflection
public data class AreaPrice(
    val coordinates: Coordinates,
    val pricePerM2: Double,
)
