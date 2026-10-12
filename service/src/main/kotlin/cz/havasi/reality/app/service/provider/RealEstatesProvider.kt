package cz.havasi.reality.app.service.provider

import cz.havasi.reality.app.model.Apartment
import cz.havasi.reality.app.model.command.GetRealEstatesCommand

public interface RealEstatesProvider {
    public suspend fun getRealEstates(getRealEstatesCommand: GetRealEstatesCommand): List<Apartment>

    // only these are worth re-reading to fill in missing coordinates
    public val providesCoordinates: Boolean get() = false
}
