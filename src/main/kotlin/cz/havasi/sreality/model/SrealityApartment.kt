import com.fasterxml.jackson.annotation.JsonProperty

internal data class SrealityApartment(
    @JsonProperty("hash_id") val hashId: String,
    @JsonProperty("advert_name") val name: String,
    @JsonProperty("price_czk") val price: Double,
    @JsonProperty("price_czk_m2") val pricePerM2: Double?,
    @JsonProperty("price_currency_cb") val currency: SrealityProperty,
    @JsonProperty("locality") val locality: SrealityLocality,
    @JsonProperty("category_main_cb") val mainCategory: SrealityProperty?,
    @JsonProperty("category_sub_cb") val subCategory: SrealityProperty?,
    @JsonProperty("category_type_cb") val transactionType: SrealityProperty?,
    @JsonProperty("advert_images") val images: List<String>,
)

internal data class SrealityLocality(
    @JsonProperty("city") val city: String,
    @JsonProperty("district") val district: String?,
    @JsonProperty("street") val street: String?,
    @JsonProperty("streetnumber") val streetNumber: String?,
    @JsonProperty("gps_lat") val latitude: Double?,
    @JsonProperty("gps_lon") val longitude: Double?
)

internal data class SrealityProperty(
    val name: String,
    val value: String
)
