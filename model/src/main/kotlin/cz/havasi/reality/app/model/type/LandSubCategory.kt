package cz.havasi.reality.app.model.type

import io.quarkus.runtime.annotations.RegisterForReflection

@RegisterForReflection
public enum class LandSubCategory(public val label: String) {
    BUILDING_PLOT("Building plot"),
    COMMERCIAL("Commercial"),
    FIELD("Field"),
    MEADOW("Meadow"),
    FOREST("Forest"),
    GARDEN("Garden"),
    ORCHARD_VINEYARD("Orchard / vineyard"),
    POND("Pond"),
    OTHER("Other"),
    ;

    public companion object {
        public fun fromValueOrNull(value: String?): LandSubCategory? = entries.firstOrNull { it.name == value }
    }
}
