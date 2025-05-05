package cz.havasi.reality.app.model.command

import cz.havasi.reality.app.model.Apartment
import cz.havasi.reality.app.model.ApartmentDuplicate

public data class UpdateApartmentWithDuplicateCommand(
    val apartment: Apartment,
    val duplicate: ApartmentDuplicate,
)
