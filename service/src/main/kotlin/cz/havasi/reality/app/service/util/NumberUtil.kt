package cz.havasi.reality.app.service.util

import kotlin.math.abs

internal fun areDoublesEqualWithTolerance(a: Double, b: Double, tolerance: Double = 0.05): Boolean =
    abs(a - b) <= abs(a).coerceAtLeast(abs(b)) * tolerance
