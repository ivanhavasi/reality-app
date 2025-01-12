package cz.havasi.service.util

public fun String.firstCapitalOthersLowerCase() =
    lowercase().replaceFirstChar { it.uppercase() }
