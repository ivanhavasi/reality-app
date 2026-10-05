package cz.havasi.reality.app.service.util

import kotlin.math.abs

internal fun areDoublesEqualWithTolerance(a: Double, b: Double, tolerance: Double = 0.05): Boolean =
    abs(a - b) <= abs(a).coerceAtLeast(abs(b)) * tolerance

// building plots only: forests and meadows genuinely sell at 10–30 Kč/m²; the lower bound excludes price on request and 1 Kč placeholders
public fun looksLikePricePerM2(price: Double, sizeInM2: Double): Boolean =
    price in 100.0..60_000.0 && price / sizeInM2 < 200
