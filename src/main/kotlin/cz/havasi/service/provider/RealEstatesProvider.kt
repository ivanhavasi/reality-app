package cz.havasi.service.provider

import cz.havasi.model.Apartment
import cz.havasi.model.command.GetRealEstatesCommand

public interface RealEstatesProvider {
    public suspend fun getRealEstates(getRealEstatesCommand: GetRealEstatesCommand): List<Apartment>
}
