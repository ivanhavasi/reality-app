package cz.havasi.service.provider

import cz.havasi.model.Apartment
import cz.havasi.model.command.GetEstatesCommand

public fun interface EstatesProvider {
    public suspend fun getEstates(getEstatesCommand: GetEstatesCommand): List<Apartment>
}
