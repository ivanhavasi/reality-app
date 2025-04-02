package cz.havasi.service.util

import kotlin.math.abs

internal fun Double.formatToNumberWithSpaces(): String =
    "%,.0f".format(this).replace(',', ' ')

internal fun areDoublesEqualWithTolerance(a: Double, b: Double, tolerance: Double = 0.05): Boolean {
    val difference = abs(a - b)

    return difference <= abs(a).coerceAtLeast(abs(b)) * tolerance
}
