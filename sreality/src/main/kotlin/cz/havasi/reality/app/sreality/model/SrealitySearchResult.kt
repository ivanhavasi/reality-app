package cz.havasi.reality.app.sreality.model

import io.quarkus.runtime.annotations.RegisterForReflection

@RegisterForReflection
internal data class SrealitySearchResult(
    val results: List<SrealityApartment>,
    val meta_description: String?,
)
