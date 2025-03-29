package cz.havasi.service.provider

import cz.havasi.model.Apartment
import cz.havasi.model.command.GetEstatesCommand
import cz.havasi.model.enum.ProviderType

public interface EstatesProvider {
    public suspend fun getEstates(getEstatesCommand: GetEstatesCommand): List<Apartment>
}
