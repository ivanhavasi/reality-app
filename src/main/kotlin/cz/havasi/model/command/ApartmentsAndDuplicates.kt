package cz.havasi.model.command

import cz.havasi.model.Apartment

public data class ApartmentsAndDuplicates(
    val apartments: List<Apartment>,
    val duplicates: List<UpdateApartmentWithDuplicateCommand>,
)
