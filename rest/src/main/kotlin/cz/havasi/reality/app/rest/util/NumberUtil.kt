package cz.havasi.reality.app.rest.util

internal fun Double.formatToNumberWithSpaces(): String =
    "%,.0f".format(this).replace(',', ' ')
