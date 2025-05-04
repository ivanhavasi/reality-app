package cz.havasi.reality.app.service.util

public fun String.firstCapitalOthersLowerCase(): String =
    lowercase().replaceFirstChar { it.uppercase() }
