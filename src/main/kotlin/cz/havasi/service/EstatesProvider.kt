package cz.havasi.service

import cz.havasi.model.Apartment
import cz.havasi.model.GetEstatesCommand

public fun interface EstatesProvider {
    public suspend fun getEstates(getEstatesCommand: GetEstatesCommand): List<Apartment>
}
