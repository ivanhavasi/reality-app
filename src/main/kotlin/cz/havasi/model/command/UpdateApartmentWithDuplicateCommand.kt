package cz.havasi.model.command

import cz.havasi.model.Apartment
import cz.havasi.model.ApartmentDuplicate

public data class UpdateApartmentWithDuplicateCommand(
    val apartment: Apartment,
    val duplicate: ApartmentDuplicate,
)
