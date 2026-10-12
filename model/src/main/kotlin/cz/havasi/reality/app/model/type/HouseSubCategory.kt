package cz.havasi.reality.app.model.type

import io.quarkus.runtime.annotations.RegisterForReflection

@RegisterForReflection
public enum class HouseSubCategory(public val label: String) {
    FAMILY_HOUSE("Family house"),
    VILLA("Villa"),
    COTTAGE("Cottage"),
    MULTI_GENERATIONAL("Multi-generational house"),
    FARMSTEAD("Farmstead"),
    TURNKEY("Turnkey project"),
    OTHER("Other"),
    ;

    public companion object {
        public fun fromValueOrNull(value: String?): HouseSubCategory? = entries.firstOrNull { it.name == value }
    }
}
