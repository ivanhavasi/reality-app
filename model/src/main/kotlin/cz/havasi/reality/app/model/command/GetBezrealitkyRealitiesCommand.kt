package cz.havasi.reality.app.model.command

public data class GetBezrealitkyRealitiesCommand(
    val offerType: String,
    val estateType: String,
    val osmValue: String,
    val regionOsmIds: String,
    val currency: String,
    val location: String,
    val page: Int,
)
