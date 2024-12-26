package cz.havasi.client.model

import SrealityApartment

internal data class SrealitySearchResult(
    val results: List<SrealityApartment>,
    val meta_description: String?,
)
