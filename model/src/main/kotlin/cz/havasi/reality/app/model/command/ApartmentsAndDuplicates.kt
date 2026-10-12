package cz.havasi.reality.app.model.command

import cz.havasi.reality.app.model.Apartment

public data class ApartmentsAndDuplicates(
    val apartments: List<Apartment>,
    val duplicates: List<UpdateApartmentWithDuplicateCommand>,
    // already stored listings that were seen again with coordinates they lack; not new, so never notified
    val locationUpdates: List<Apartment> = emptyList(),
)
