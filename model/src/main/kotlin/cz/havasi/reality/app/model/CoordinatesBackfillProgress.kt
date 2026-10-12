package cz.havasi.reality.app.model

public data class CoordinatesBackfillProgress(
    // phase 1: portals that send coordinates are re-read
    val pagesScanned: Int = 0,
    val listingsSeen: Int = 0,
    val coordinatesFilled: Int = 0,
    // phase 2: listings still without coordinates are located from their street address
    val addressesToLocate: Int = 0,
    val addressesChecked: Int = 0,
    val addressesLocated: Int = 0,
)
