package cz.havasi.service.util

public fun Double.formatToNumberWithSpaces(): String =
    "%,.0f".format(this).replace(',', ' ')
