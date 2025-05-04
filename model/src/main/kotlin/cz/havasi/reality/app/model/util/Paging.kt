package cz.havasi.reality.app.model.util

public data class Paging(
    val sortBy: String = "updatedAt",
    val sortDirection: SortDirection = SortDirection.DESC,
    val offset: Int = 0,
    val limit: Int = 20,
)

public enum class SortDirection(public val value: Int) {
    ASC(1),
    DESC(-1),
}
