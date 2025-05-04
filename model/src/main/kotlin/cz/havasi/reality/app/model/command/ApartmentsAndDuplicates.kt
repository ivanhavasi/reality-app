package cz.havasi.reality.app.model.command

import cz.havasi.reality.app.model.Apartment

public data class ApartmentsAndDuplicates(
    val apartments: List<Apartment>,
    val duplicates: List<UpdateApartmentWithDuplicateCommand>,
)
