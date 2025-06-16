package cz.havasi.reality.app.model.util

import io.quarkus.runtime.annotations.RegisterForReflection

@RegisterForReflection
public data class Paging(
    val sortBy: String = "updatedAt",
    val sortDirection: SortDirection = SortDirection.DESC,
    val offset: Int = 0,
    val limit: Int = 20,
)

@RegisterForReflection
public enum class SortDirection(public val value: Int) {
    ASC(1),
    DESC(-1),
}
