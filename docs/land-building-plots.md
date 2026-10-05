# Land / Building Plots (stavební pozemky): Implementation Plan

| | |
|---|---|
| **Status** | Research, decisions and validation done (2026-10-05). Not implemented yet. |
| **Covers** | `reality-app` (backend) and `reality-app-fe` (frontend) |
| **Evidence** | Live probes of sreality.cz, bezrealitky.cz and reality.idnes.cz on 2026-10-04/05 (app User-Agent, polite delays). A full replay of the proposed parsing and dedup on live Prague data. A Kotlin prototype of the core backend changes with 25 passing unit tests. Framework behaviour checked against Quarkus 3.17.5 bytecode and a probe app. |

Paths are relative to `reality-app/` unless prefixed `FE:`, which means `reality-app-fe/src/`.

---

## 1. TL;DR

- **Feasible, medium-sized change, validated on live Prague data.**
  - Today there are 465 Prague building-plot listings: Sreality 237, iDNES 217, Bezrealitky 11.
  - The proposed parsing reads area and subtype correctly for every one of them, and it builds working URLs on all three portals.
- **DB:** no data migration, no backfill and no schema change (verified, §8).
  - One **index-only** change unit (`006`) is needed for the recommended `duplicates.id` lookup.
  - Any new persisted field must be nullable (verified).
- **Duplicate detection is the critical part.**
  - The current scheme would silently drop **56** distinct Prague plots and wrongly attach **43**.
  - The new scheme matches on city + exact area, with a price and city-part guard. On the same data it makes **189 correct merges, 0 wrong**, and misses 2.
- **Bezrealitky per-m² prices typed in as totals can be detected, and the full price (price × area) can be calculated.**
  - It is rare: 2 of 288 building plots across CZ, 0 of 11 in Prague.
  - Phase 1 detects these listings, skips them and logs them (§6.4).
- **Problems you should know about** (§15):
  - **A pre-existing security bug, verified:** any logged-in user can read and modify any other user's notifications.
  - A config pitfall that would have stopped **all** scraping. The prototype caught it, and the doc now avoids it.
  - An iDNES decimal-comma price bug that also affects apartments.
  - About 2% of iDNES "building plots" are fractional shares, and the cards look identical to normal plots.

---

## 2. Decisions

| # | Topic | Decision | Consequence |
|---|---|---|---|
| D1 | Region | **Prague only** | Every provider's existing Prague hardcoding stays. Expanding beyond Prague is future work (§16). |
| D2 | Subtypes | **Building plots (`BUILDING_PLOT`) only** | All subtypes are modelled, but only `BUILDING_PLOT` is scraped. The FE shows no subtype chips while there is only one. |
| D3 | Transaction | **Sale only** | Land rent is negligible: Sreality has 0 building plots for rent in Prague. |
| D4 | Price on request | **Same as apartments** | Stored with `price = 0` and `pricePerM2 = 0`. Notified under the existing rules, so it matches any filter without `price.from`. Displayed like apartments, with no special label. This affects 12.4% of Prague plots (§12.1). |
| D5 | Bezrealitky per-m² price typed as total | **Detect, skip and log in phase 1.** Correcting the price is an optional extension (§6.4). | This is my recommendation from the research. **Confirm or flip it** (§16). |
| D6 | UI language | **Keep the current English UI** | Labels like "Land" and "Building plot". |

---

## 3. Scope

**In:**
- LAND × SALE × BUILDING_PLOT in Prague.
- Scraping, dedup, search, detail page, map, alerts and stats.

**Out:**
- Other land subtypes. They are modelled and can be enabled later.
- LAND × RENT.
- Regions outside Prague.
- HOUSE. It has the same Sreality bugs (§15.1).
- Auctions and shares, where the portal separates them. Sreality uses `category_type_cb` 3 and 4 for those. **iDNES does not separate shares** (§6.3).
- Enrichment from detail pages.

---

## 4. What exists today

| Layer | Already land-ready | Apartment-only |
|---|---|---|
| Model | `BuildingType.LAND`. `subCategory` is a free `String?`, and `pricePerM2` is nullable. | – |
| Providers | All 3 map LAND to the portal's category. | All parsing (§6). Sreality hardcodes `APARTMENT` in the fingerprint and `/byt/` in the URL. |
| Scheduler | – | Only APARTMENT SALE and RENT (`RealityScheduler.kt:21,29`). |
| REST | `building=LAND` is accepted. Statistics accept `buildingType` and `subCategory`. | `sizeMax` defaults to 1000 (`RealEstateController.kt:47`). There is no subCategory search filter. `/process` is hardcoded to APARTMENT SALE (`:35`). |
| Mongo | Enums are stored as strings, so LAND round-trips. | Statistics only count `5 < sizeInM2 < 10000` (`MongoClientApartmentRepository.kt:333-334`). |
| Notifications | The filter has `buildingType` and `subTypes`. Matching is type-agnostic. | Templates say "Apartment" and print raw codes and `null`s. |
| FE | A Building Type select exists, hidden under Advanced Search. | The size slider maxes out at 300. Cards show raw codes. A null `pricePerM2` crashes the detail page. AddNotification has no LAND option and offers `COMMERCIAL`, which the backend rejects. |

---

## 5. Domain design

### 5.1 Canonical `LandSubCategory`

```kotlin
// model/src/main/kotlin/cz/havasi/reality/app/model/type/LandSubCategory.kt
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
```

`Apartment.subCategory` stores `LandSubCategory.name`.
- UPPER_SNAKE values cannot clash with apartment dispositions like `"2+kk"`.
- The same string is used everywhere: fingerprint inputs, notification `subTypes`, the FE query param, and the FE label key.
- The FE uses **identical labels** (§11.6).

**Provider mapping** (all verified live). Phase 1 scrapes only the first row.

| Canonical | Sreality `category_sub_cb` (name → detail slug) | Bezrealitky `landType` | iDNES path slug / title noun |
|---|---|---|---|
| `BUILDING_PLOT` | **19** Bydlení → `bydleni` | **`STAVEBNI`** | **`stavebni-pozemek`** / "stavebního pozemku" |
| `COMMERCIAL` | 18 Komerční → `komercni` | `KOMERCNI` | `pro-komercni-vyuziti` / "komerčního pozemku" |
| `FIELD` | 20 Pole → `pole` | `POLE` | `zemedelsky` / "pole" |
| `MEADOW` | 22 Louky → `louka` | `LOUKA` | `louka` / "louky" |
| `FOREST` | 21 Lesy → `les` | `LES` | `les` / "lesa" |
| `GARDEN` | 23 Zahrady → `zahrada` | `ZAHRADA` | `zahrady` / "zahrady" |
| `ORCHARD_VINEYARD` | 48 Sady/vinice → `sady-vinice` | – | – |
| `POND` | 46 Rybníky → `rybnik` | `RYBNIK` | `vodni-plocha` / "vodní plochy" |
| `OTHER` | 24 Ostatní → `ostatni-pozemky` | `OSTATNI` | `jine` / "pozemku" (bare) |

Gotchas:
- Sreality silently ignores unknown sub codes and returns all land instead.
- iDNES `/stavebni-parcela/` returns 404.
- Bezrealitky `UNDEFINED` means "no filter".

### 5.2 Field semantics for LAND

| Field | Meaning |
|---|---|
| `sizeInM2` | Plot area. It is an exact cadastral integer on all three portals; verified on 465/465 Prague listings (§12.1). **Listings whose area can't be parsed are skipped.** |
| `price` | Always the **total**. Price on request is stored as `0` (D4). |
| `pricePerM2` | `price / sizeInM2`, so it is `0` for price on request, exactly like apartments. It is never null for land, because listings without a size are skipped. |
| `subCategory` | `LandSubCategory.name` |
| `locality.city` | `"Praha"` (verified on all three portals, §12.1). |
| `locality.district` | The **city part**, e.g. "Radotín" or "Dolní Chabry", not "Praha 5". The dedup guard needs it (§7.3). |

### 5.3 Command & service plumbing

```kotlin
public data class GetRealEstatesCommand(
    val type: BuildingType,
    val transaction: TransactionType,
    val offset: Int,
    val limit: Int,
    val landSubCategory: LandSubCategory? = null,
)
```

- **One subtype per fetch, for two reasons:**
  - Bezrealitky results never reveal the land type. They say `landType: "UNDEFINED"` even when the search is filtered, so the provider tags each result with the subtype it requested.
  - iDNES needs a different URL form for one subtype than for several.
- A provider returns `emptyList()` for a subtype it doesn't support.
- The value is passed through `RealEstateService.fetchAndSaveRealEstate` → `fetchAndSaveApartmentsForProvider` (`RealEstateService.kt:27-31, 40-61`) → `getApartments` (`:63-76`).

---

## 6. Providers

### 6.0 Summary (Prague building plots, 2026-10-05)

| | Sreality | Bezrealitky | iDNES |
|---|---|---|---|
| Listings | 237 | 11 | 217 |
| Transport | JSON API `/api/v1/estates/search` | HTML (Next.js, hashed CSS classes) | HTML |
| Building-plot filter | `category_sub_cb=19` | `landType=STAVEBNI` | path `/s/prodej/pozemky/stavebni-pozemek/praha/` |
| Subtype visible in results | yes (code) | **no**, taken from the request | yes (title noun) |
| Area source | regex on `advert_name` | card `li` ("217 m²") | regex on the title |
| Price on request (0) | 30 (12.7%) | 0 | 27 (12.4%) |
| Per-m² trap | 11 (4.6%) have `price_unit_cb=3`; fixed via `price_summary_czk` | Seller typed the per-m² price as the total (§6.4); none in Prague today | Bezrealitky's syndicated copies (§6.4) |
| No street | 29% | 36% | 41% |
| GPS | 100% | only in `__NEXT_DATA__` | none |
| `city` value | `"Praha"` 237/237 | hardcoded `"Praha"` | hardcoded `"Praha"` |
| City part (→ `district`) | `locality.citypart` | text after `" - "` | text after `" - "` |
| Default order | roughly by `edited`, newest first ("bump") | `TIMEORDER_DESC` | by bump, not creation |
| Page size | 22 (requested) | 15 | 25 |

### 6.1 Sreality

**What breaks today**

| Line | Problem |
|---|---|
| `:78` | The fingerprint hardcodes `BuildingType.APARTMENT`. |
| `:80, :92` | `subCategory` is the raw Czech name ("Bydlení"). |
| `:85` | `price = price_czk`, which is the per-m² value for "za m²" listings. |
| `:87, :99-106` | size = price / pricePerM2. That gives **1 m²** for per-m² listings and **0** for price 0. |
| `:108-109` | The URL `/detail/prodej/byt/...` returns **404** for land (verified). |
| `:31-36` | Any mapping exception drops the whole page of 22. |

**Changes.** Land gets its own mapping function, so apartment output is untouched.

```kotlin
// SrealityApi.searchEstates: new param (null is omitted from the URL, verified §12.3)
@QueryParam("category_sub_cb") categorySub: String?,

// SrealityApartment: new fields
@JsonProperty("price_summary_czk") val priceSummary: Double?,
@JsonProperty("price_unit_cb") val priceUnit: SrealityProperty?,

// SrealityLocality: new field
@JsonProperty("citypart") val citypart: String?,
```

```kotlin
// sreality/.../model/SrealityLandSubCategory.kt
internal enum class SrealityLandSubCategory(val code: Int, val canonical: LandSubCategory, val detailSlug: String) {
    BUILDING_PLOT(19, LandSubCategory.BUILDING_PLOT, "bydleni"),
    COMMERCIAL(18, LandSubCategory.COMMERCIAL, "komercni"),
    FIELD(20, LandSubCategory.FIELD, "pole"),
    FOREST(21, LandSubCategory.FOREST, "les"),
    MEADOW(22, LandSubCategory.MEADOW, "louka"),
    GARDEN(23, LandSubCategory.GARDEN, "zahrada"),
    OTHER(24, LandSubCategory.OTHER, "ostatni-pozemky"),
    POND(46, LandSubCategory.POND, "rybnik"),
    ORCHARD_VINEYARD(48, LandSubCategory.ORCHARD_VINEYARD, "sady-vinice"),
    ;

    companion object {
        fun fromCode(code: String?): SrealityLandSubCategory? = entries.firstOrNull { it.code.toString() == code }
        fun from(canonical: LandSubCategory): SrealityLandSubCategory = entries.first { it.canonical == canonical }
    }
}
```

```kotlin
// SrealityRealEstatesProvider
// callClient(): add `categorySub = resolveCategorySub()` and replace `.map { it.toApartment(this) }` with
//   .mapNotNull { estate ->
//       runCatching { if (type == BuildingType.LAND) estate.toLand(this) else estate.toApartment(this) }
//           .onFailure { e -> Log.warn("Skipping Sreality estate ${estate.hashId}", e) }
//           .getOrNull()
//   }
// (mapNotNull alone doesn't catch; this also stops one bad apartment from dropping its page)

private fun GetRealEstatesCommand.resolveCategorySub(): String? =
    if (type == BuildingType.LAND) landSubCategory?.let { SrealityLandSubCategory.from(it).code.toString() } else null

private val AREA_REGEX = Regex("""(\d+)\s*m²""")
private const val PRICE_UNIT_PER_M2 = "3"
private const val MAX_PLAUSIBLE_PRICE_PER_M2 = 60_000.0

private fun SrealityApartment.toLand(command: GetRealEstatesCommand): Apartment? {
    val size = AREA_REGEX.findAll(name).lastOrNull()?.groupValues?.get(1)?.toDouble()
        ?: return null.also { Log.warn("Sreality land $hashId has no area in '$name', skipping") }
    val price = resolveLandPrice()
    val locality = locality.toLandLocality()
    val subCategory = SrealityLandSubCategory.fromCode(subCategory?.value)?.canonical?.name
    return Apartment(
        id = hashId,
        fingerprint = constructFingerprint(BuildingType.LAND, locality, subCategory ?: "", command.transaction, size),
        name = name,
        url = prepareLandUrl(command.transaction),
        price = price,
        pricePerM2 = price / size,
        sizeInM2 = size,
        currency = currency.name.toCurrencyType(),
        locality = locality,
        mainCategory = BuildingType.LAND,
        subCategory = subCategory,
        transactionType = command.transaction,
        images = images.map { "https:$it?fl=res,800,600,3|shr,,20|webp,60" },
        provider = ProviderType.SREALITY,
    )
}

// "za m²" listings carry the per-m² price in price_czk and the total in price_summary_czk.
// A "za m²" price above 60 000 is a total typed into the per-m² field (seen once in Středočeský, 2026-10-05).
private fun SrealityApartment.resolveLandPrice(): Double =
    if (priceUnit?.value == PRICE_UNIT_PER_M2 && price <= MAX_PLAUSIBLE_PRICE_PER_M2) priceSummary ?: price else price

private fun SrealityLocality.toLandLocality() =
    Locality(
        city = city,
        district = citypart?.ifBlank { null },
        street = street?.ifBlank { null },
        streetNumber = streetNumber?.ifBlank { null },
        latitude = latitude,
        longitude = longitude,
    )

private fun SrealityApartment.prepareLandUrl(transaction: TransactionType): String {
    val transactionSlug = if (transaction == TransactionType.SALE) "prodej" else "pronajem"
    val subSlug = SrealityLandSubCategory.fromCode(subCategory?.value)?.detailSlug ?: "bydleni"
    return "$baseUrl/detail/$transactionSlug/pozemek/$subSlug/${locality.citySeoName ?: ""}-${locality.citypartSeoName ?: ""}-${locality.streetSeoName ?: ""}/$hashId"
}
```

Validated on live data:
- Area: 237/237 equal the detail endpoint's `estate_area`.
- "za m²" totals: 10/10 exact.
- URLs: 6/6 work. One with an empty locality returns 301, then 200.

### 6.2 Bezrealitky

**What breaks today**

| Line | Problem |
|---|---|
| `:63-64` | size = `li[1]`. Land cards have only one `li`, so size falls back to 1.0. |
| `:65-66` | subCategory = `li[0]` = `"217 m²"`. The "Ostatní" → "atypicky" mapping only makes sense for apartments. |
| `:91` | pricePerM2 = price / 1.0, so it equals the full price. |
| `:55-60` | When there is no street: street = "Praha - Ďáblice", district = "Unknown district", and an error is logged. |

**Changes.** Land gets its own card mapping, so apartments are untouched.

```kotlin
// BezrealitkyApi.searchEstates: new param
@QueryParam("landType") landType: String?,
```

```kotlin
// BezrealitkyRealEstatesProvider
// getRealEstates(): listings don't carry the land type, so an unfiltered LAND fetch couldn't be classified
if (getRealEstatesCommand.type == BuildingType.LAND && getRealEstatesCommand.prepareLandType() == null) return emptyList()
// callClient(): landType = prepareLandType()
// parseDataFromResponse(): `.map {` -> `.mapNotNull { card -> if (type == BuildingType.LAND) card.toLand(this) else <existing body> }`

private fun GetRealEstatesCommand.prepareLandType(): String? =
    if (type != BuildingType.LAND) null else when (landSubCategory) {
        LandSubCategory.BUILDING_PLOT -> "STAVEBNI"
        LandSubCategory.COMMERCIAL -> "KOMERCNI"
        LandSubCategory.FIELD -> "POLE"
        LandSubCategory.MEADOW -> "LOUKA"
        LandSubCategory.FOREST -> "LES"
        LandSubCategory.GARDEN -> "ZAHRADA"
        LandSubCategory.POND -> "RYBNIK"
        LandSubCategory.OTHER -> "OSTATNI"
        LandSubCategory.ORCHARD_VINEYARD, null -> null
    }

private fun Element.toLand(command: GetRealEstatesCommand): Apartment? {
    val url = selectFirst(".PropertyCard_propertyCardHeadline___diKI")?.selectFirst("a")?.attr("href") ?: return null
    val id = url.substringAfter("/nemovitosti-byty-domy/")
    val size = getElementsByClass("FeaturesList_featuresList__75Wet").select("li")
        .map { it.text().trim() }
        .firstOrNull { it.endsWith("m²") }
        ?.parseSquareMeters()
        ?: return null.also { Log.warn("Bezrealitky land $id has no area, skipping") }
    val price = selectFirst(".PropertyPrice_propertyPriceAmount__WdEE1")?.text()?.substringBefore("Kč")
        ?.filter { it.isDigit() }?.toDoubleOrNull() ?: 0.0
    if (looksLikePricePerM2(price, size)) {
        Log.warn("Bezrealitky land $id: $price Kč for $size m² looks like a per-m² price, skipping")
        return null
    }
    val locality = selectFirst(".PropertyCard_propertyCardAddress__hNqyR")?.text()?.toLandLocality() ?: return null
    return Apartment(
        id = id,
        fingerprint = constructFingerprint(BuildingType.LAND, locality, "", command.transaction, size),
        name = selectFirst(".PropertyCard_propertyCardHeadline___diKI")?.text() ?: id,
        url = url,
        price = price,
        pricePerM2 = price / size,
        sizeInM2 = size,
        currency = CurrencyType.CZK,
        locality = locality,
        mainCategory = BuildingType.LAND,
        subCategory = command.landSubCategory?.name,
        transactionType = command.transaction,
        images = parseImages(), // extract the existing image parsing (:49-51) into a helper
        provider = ProviderType.BEZREALITKY,
    )
}

private fun String.parseSquareMeters(): Double? =
    substringBefore("m²").filter { it.isDigit() || it == ',' }.replace(',', '.').toDoubleOrNull()

// Prague: "[Street, ]Praha - Part"
private fun String.toLandLocality(): Locality {
    val parts = split(",").map { it.trim() }
    return Locality(
        city = "Praha",
        district = parts.last().substringAfter(" - ", "").ifBlank { null },
        street = parts.takeIf { it.size > 1 }?.first(),
        streetNumber = null, latitude = null, longitude = null,
    )
}
```

Validated on live data:
- 11/11 sizes equal `surfaceLand`.
- 11/11 prices equal the `__NEXT_DATA__` price.
- 11/11 addresses parse.

**End of results.** In the probe, Bezrealitky answered page 2 with a **307 redirect to `/vypis/?page=1`**, which is a generic feed of apartments, rentals and offices.
- Redirects are not followed by default (verified, §12.3), so that page yields 0 cards and the loop stops.
- **Never set `follow-redirects` on this client.** If you did, the land parser would store "3+kk 75 m²" as a 75 m² Prague plot.
- Optional belt-and-braces check: if the page's `__NEXT_DATA__` `listAdverts(...)` key doesn't contain `"landType":["STAVEBNI"]`, return an empty list.

**Optional later: parse `__NEXT_DATA__` instead of the CSS classes.**
- It is in the same response.
- It gives `surfaceLand`, GPS and images.
- It doesn't depend on hashed class names, which change on site rebuilds.
- It needs an injected `ObjectMapper`.

### 6.3 iDNES Reality

**What breaks today**

| Line | Problem |
|---|---|
| `:74` | size = title word[3]. This is wrong for 30–60% of land cards: "Prodej pole 24 066 m²" gives 66, and one-word types give 1.0. |
| `:77-78` | subCategory = word[2], which comes out as `"pozemku"` or a number. |
| `:75` | `price / size` has no zero guard. |
| `:68-72` | When there is no street, parsing breaks the same way as on Bezrealitky. |
| `IdnesApi.kt:17` | No path segment for the subtype. |
| `:112-125` | **Decimal-comma price bug, which also hits apartments.** "17 999 999,44 Kč" parses as 0 (seen on `69e77362842fad586b02a7da`). |

**Changes**

```kotlin
// IdnesApi: new method (the unused searchEstatesForOtherPages at :26-34 can go)
@GET
@Path("/{transactionType}/{buildingType}/{subType}/{location}/")
@Produces(MediaType.TEXT_HTML)
suspend fun searchEstatesWithSubType(
    @PathParam("transactionType") transactionType: String,
    @PathParam("buildingType") buildingType: String,
    @PathParam("subType") subType: String,
    @PathParam("location") location: String,
    @QueryParam("page") page: Int?,
): RestResponse<String>
```

```kotlin
// IdnesRealEstateProvider
// callClient(): use searchEstatesWithSubType when type == LAND; return emptyList() when idnesSlug is null.
// parseDataFromResponse(): `.map {` -> `.mapNotNull { card -> if (type == BuildingType.LAND) card.toLand(this) else <existing body> }`

private val LandSubCategory.idnesSlug: String?
    get() = when (this) {
        LandSubCategory.BUILDING_PLOT -> "stavebni-pozemek"
        LandSubCategory.COMMERCIAL -> "pro-komercni-vyuziti"
        LandSubCategory.FIELD -> "zemedelsky"
        LandSubCategory.FOREST -> "les"
        LandSubCategory.GARDEN -> "zahrady"
        LandSubCategory.MEADOW -> "louka"
        LandSubCategory.POND -> "vodni-plocha"
        LandSubCategory.OTHER -> "jine"
        LandSubCategory.ORCHARD_VINEYARD -> null
    }

// (?:^|\s) stops "2+1 65 m²" being read as 165
private val SIZE_REGEX = Regex("""(?:^|\s)(\d{1,3}(?:[\s ]\d{3})*(?:,\d+)?)\s*m²""")

private val LAND_TITLE_NOUNS = mapOf(
    "stavebního pozemku" to LandSubCategory.BUILDING_PLOT,
    "komerčního pozemku" to LandSubCategory.COMMERCIAL,
    "zahrady" to LandSubCategory.GARDEN,
    "pole" to LandSubCategory.FIELD,
    "lesa" to LandSubCategory.FOREST,
    "louky" to LandSubCategory.MEADOW,
    "vodní plochy" to LandSubCategory.POND,
    "pozemku" to LandSubCategory.OTHER,
)

private fun String.parseSize(): Double? =
    SIZE_REGEX.find(this)?.groupValues?.get(1)?.filter { it.isDigit() || it == ',' }?.replace(',', '.')?.toDoubleOrNull()

// "Prodej stavebního pozemku 1 418 m²" -> "stavebního pozemku"
private fun String.parseLandSubCategory(): LandSubCategory? =
    lowercase().split(" ").drop(1)
        .takeWhile { word -> word.firstOrNull()?.isLetter() == true }
        .joinToString(" ")
        .let { LAND_TITLE_NOUNS[it] }

private fun Element.toLand(command: GetRealEstatesCommand): Apartment? {
    val linkElement = selectFirst("a.c-products__link") ?: return null
    val url = linkElement.attr("href")
    val id = url.split("/").let { it.getOrNull(it.lastIndex - 1) } ?: return null
    val name = selectFirst(".c-products__title")?.text()?.firstCapitalOthersLowerCase() ?: return null
    val size = name.parseSize() ?: return null.also { Log.warn("iDnes land $id has no area in '$name', skipping") }
    val price = getPrice(command.transaction)
    if (looksLikePricePerM2(price, size)) { // Bezrealitky exports their adverts here; also catches tiny-share listings
        Log.warn("iDnes land $id: $price Kč for $size m² looks like a per-m² price, skipping")
        return null
    }
    val locality = selectFirst(".c-products__info")?.text()?.toLandLocality() ?: return null
    val subCategory = (name.parseLandSubCategory() ?: command.landSubCategory)?.name
    return Apartment(
        id = id,
        fingerprint = constructFingerprint(BuildingType.LAND, locality, subCategory ?: "", command.transaction, size),
        name = name,
        url = url,
        price = price,
        pricePerM2 = price / size,
        sizeInM2 = size,
        currency = CurrencyType.CZK,
        locality = locality,
        mainCategory = BuildingType.LAND,
        subCategory = subCategory,
        transactionType = command.transaction,
        images = listOfNotNull(selectFirst(".c-products__img img")?.attr("data-src")),
        provider = ProviderType.IDNES,
    )
}

// Prague: "[Ulice, ]Praha[ N][ - Část]". Outside Prague, normalise ^Praha( \d+)?$ to the municipality instead.
private fun String.toLandLocality(): Locality {
    val parts = split(",").map { it.trim() }.filter { it.isNotEmpty() }
    return Locality(
        city = "Praha",
        district = parts.last().substringAfter(" - ", "").ifBlank { null },
        street = parts.takeIf { it.size > 1 }?.first(),
        streetNumber = null, latitude = null, longitude = null,
    )
}

// getPrice(): fix the decimal comma (affects apartments too)
//     ?.replace(" ", "")
//     ?.replace(",", ".")      // "17 999 999,44 Kč"
//     ?.trim()
//     ?.toDoubleOrNull()
```

`looksLikePricePerM2` is the same rule as in §6.4. Share it from `service/util`, or duplicate it in both providers.

Validated on live data:
- 217/217 sizes parse. Price ÷ size equals the site's own "(N Kč/m²)" figure.
- 217/217 subtypes come from the title.
- jsoup 1.18.3 `.text()` turns NBSP into a space, as the regex expects.

**End of results (verified).** Pages 9–13 return **302** to the last page; from page 14 on they return **404**.
- 302 is not followed, so `handleResponse` sees it and turns every non-200 into an empty list.
- 404 **throws `ClientWebApplicationException` before `handleResponse`**, even though the return type is `RestResponse<String>`. Catch it in `getRealEstates` and log it at debug level:
  ```kotlin
  } catch (e: WebApplicationException) {
      if (e.response.status != 404) Log.error("Error while fetching iDnes data", e)
      emptyList()
  }
  ```

**Fractional shares (podíly) appear as normal plots.** At least 4 of 217 Prague listings are shares, e.g. `6a7dc9fb3d350a753a068906`: a 41.67% share at 251 Kč/m².
- The card doesn't say so; only the detail title contains "podíl".
- The per-m² rule catches the cheapest one.
- **Phase 1: accept the remaining ~2% noise** (§16).

### 6.4 Bezrealitky: per-m² price typed as the total

**Can we detect it? Yes, heuristically.** Bezrealitky has no explicit unit signal:
- The seller form (`SaveAdvertInput` in the GraphQL schema) has only `price` and `currency`.
- Every "per m²" figure the site shows is just price ÷ area, e.g. "11 000 Kč (3 Kč / m²)".

| Rule | CZ Bezrealitky building plots (288) | Prague Bezrealitky (11) | iDNES Prague (190 priced) | Sreality Praha + Stč. (2 211 total-priced) |
|---|---|---|---|---|
| price/m² < 200 alone | 2 TP / **11 FP** | 0 | 0 | 0 / 5 FP |
| **100 ≤ price ≤ 60 000 && price/m² < 200** (chosen) | **2 TP / 0 FP** | 0 | 0 | **0 / 0** |

- The two real cases are 975585 (5 000 Kč for 751 m²) and 1068172 (11 000 Kč for 3 315 m²), i.e. 3–7 Kč/m².
- Genuine plots are far above that. The cheapest owner-entered building plot in CZ is 233 Kč/m², and the cheapest in Prague is 4 174 Kč/m².
- The lower bound of 100 excludes price on request (0) and 1 Kč placeholders.
- **Building plots only.** Forests and meadows genuinely sell at 10–30 Kč/m².
- **Prevalence:** 0.7% across CZ, 0 of 11 in Prague. My earlier ~4% estimate could not be reproduced.
- **Caveat:** with only 2 true cases, the miss rate is not well measured.

**Can we calculate the full price? Yes: price × area is correct.**
- It is the market convention. On Sreality, `price_summary_czk` equals per-m² × area for **216/216** "za m²" plots.
- Both flagged adverts say "N Kč/m²" in their description, where N is the listed price.
- The resulting totals (36.5 M and 3.76 M) fit neighbouring plots. For example, an independent agency lists a 750 m² plot in the same village for 3.75 M.
- No advert was found with a per-parcel price combined with a summed area.

**Why phase 1 skips instead of correcting:**
- The search card has **no description**, so a correction can't be confirmed.
- Confirming would take one extra request per suspect: the detail page's `__NEXT_DATA__`, or GraphQL `advert(id){description}`.
- With 0 Prague cases and about 1% of new adverts expected, a WARN log is the cheaper choice.
- The same adverts are **syndicated to iDNES** (`data-brand="Bezrealitky s.r.o."`), so iDNES applies the same rule and skips them too (§6.3).
- On iDNES, correcting would be unsafe: genuine shares there sit as low as 18.8 Kč/m².

**Optional extension: correct when the description confirms.** Add it later; no schema change is needed.

```kotlin
private val PRICE_PER_M2_IN_TEXT = Regex(
    """(\d{1,3}(?:[\s  .]\d{3})+|\d+)(?:,\d+)?\s*(?:,-\s*)?(?:Kč|CZK)?\s*(?:,-\s*)?(?:/|za\s*(?:1\s*)?)\s*m(?:2|²)""",
    RegexOption.IGNORE_CASE,
)

private fun String?.statesPricePerM2(price: Double): Boolean =
    this != null && PRICE_PER_M2_IN_TEXT.findAll(this).any { it.groupValues[1].filter(Char::isDigit).toDoubleOrNull() == price }

// in toLand(): instead of skipping, fetch the description for the suspect and
//   if (description.statesPricePerM2(price)) price * size  else skip
```

The regex was checked on the JVM against 288 descriptions: 26 matched, and it confirmed exactly the 2 real cases. Across 1 648 land adverts, the "number equals price" check produced 0 false positives.

---

## 7. Service layer

### 7.1 Scheduler

Land gets a separate job with its own cron, sharing a mutex with the apartment job. `SKIP` concurrency is **verified to work on `suspend` methods** (§12.3).

```kotlin
@ApplicationScoped
internal class RealityScheduler(
    private val realEstateService: RealEstateService,
) {
    private val fetchMutex = Mutex()

    @Scheduled(cron = "{reality.scheduler.cron}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    internal suspend fun scheduleRealityRetrieval() {
        Log.info("Scheduled task started")
        waitForRandomInterval()
        fetchMutex.withLock {
            fetch(BuildingType.APARTMENT, TransactionType.SALE)
            waitForRandomInterval(120)
            fetch(BuildingType.APARTMENT, TransactionType.RENT)
        }
        Log.info("Scheduled task finished")
    }

    @Scheduled(cron = "{reality.scheduler.land-cron}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    internal suspend fun scheduleLandRetrieval() {
        waitForRandomInterval(300)
        fetchMutex.withLock { fetch(BuildingType.LAND, TransactionType.SALE, LandSubCategory.BUILDING_PLOT) }
    }

    private suspend fun fetch(buildingType: BuildingType, transactionType: TransactionType, landSubCategory: LandSubCategory? = null) {
        Log.info("Fetching and saving $buildingType $transactionType ${landSubCategory ?: ""}")
        try {
            realEstateService.fetchAndSaveRealEstate(buildingType, transactionType, landSubCategory)
        } catch (e: Exception) {
            Log.error("Fetching $buildingType $transactionType failed", e)
        }
    }

    // waitForRandomInterval unchanged
}
```

```yaml
reality:
  scheduler:
    cron: "0 */30 6-23 * * ?"
    land-cron: "${LAND_SCHEDULER_CRON:off}"   # prod e.g. "0 15 6-22 * * ?"
```

> ⚠️ **Three config traps, all verified:**
> - **Never write a bare `land-cron: off`.** YAML reads it as boolean `false`, and Quarkus then fails to start. Quote it, or use the env placeholder as above. `off` and `disabled` are the only values that disable a job.
> - **If you ever make the subtypes configurable, inject `MutableList<LandSubCategory>`, not `List<LandSubCategory>`.**
>   - Kotlin compiles `List` to `List<? extends …>`, which SmallRye Config can't resolve (`SRCFG02005`).
>   - The scheduler bean then fails on **every** run, and since the apartment job lives in the same bean, **all scraping stops**.
>   - Phase 1 hardcodes `BUILDING_PLOT`, so it avoids this.
> - **`cron: "-"` in `application-dev.yaml` is not a disable value.** The probe app fails to start with it ("Cron expression contains 1 parts"). Use `"off"`.

### 7.2 Fetch loop: full Prague coverage, providers in sequence

The Prague replay (§12.1) found two problems with today's loop when applied to land:
- **The early stop misses listings.** The loop stops at the first page with nothing new. On the first run that reaches only 46% of Sreality and 58% of iDNES. Portals sort by "bump", and in steady state a genuinely unseen listing appeared at **position 43**, which the early stop would have missed.
- **Parallel providers store cross-portal pairs twice.** A plot published on two portals in the same run isn't visible to either side before both save it, and both copies are notified. Apartments already have this problem.

Prague land is small: about 240 Sreality and 220 iDNES listings, with 12–15 new ones per week. So **scan everything on every run, one provider at a time**:

```kotlin
public suspend fun fetchAndSaveRealEstate(
    buildingType: BuildingType,
    transactionType: TransactionType,
    landSubCategory: LandSubCategory? = null,
) {
    // land runs providers one after another, so a plot listed on two portals in the same run is merged, not stored twice
    if (buildingType == BuildingType.LAND) {
        realEstateProviders.forEachSequentially(message = "Fetching and saving land") {
            fetchAndSaveApartmentsForProvider(it, buildingType, transactionType, landSubCategory)
        }
    } else {
        realEstateProviders.forEachAsync(message = "Fetching and saving apartments") {
            fetchAndSaveApartmentsForProvider(it, buildingType, transactionType, landSubCategory)
        }
    }
}

private suspend fun fetchAndSaveApartmentsForProvider(
    provider: RealEstatesProvider,
    buildingType: BuildingType,
    transactionType: TransactionType,
    landSubCategory: LandSubCategory?,
) {
    val numberOfApartments = 22
    val maxCalls = if (buildingType == BuildingType.LAND) 12 else 5

    for (i in 0 until maxCalls) {
        val fetched = provider.getApartments(i * numberOfApartments, numberOfApartments, buildingType, transactionType, landSubCategory)
        if (fetched.isEmpty()) break
        val saved = fetched.filterApartments().saveApartments().sendNotifications()
        // portals reorder land by "bump", so new plots can sit past the first page: land always scans every page
        if (saved.isEmpty() && buildingType != BuildingType.LAND) {
            Log.debug("No more apartments to save for provider $provider")
            break
        }
        delay(Random.nextLong(700, 2500))
    }
}
```

- `forEachSequentially` is a new helper next to `forEachAsync` in `CoroutineUtil.kt`. It swallows exceptions per provider in the same way (prototyped).
- Apartment behaviour is unchanged: an empty fetch already led to an empty save and a break.
- **Cost per land run:** about 11 Sreality pages, 9–10 iDNES pages and 1–2 Bezrealitky pages. That is roughly 22 requests over a few minutes, once an hour.
- No separate backfill is needed, because the first run already covers everything.

### 7.3 Fingerprint & dedup (critical)

**How it works today:**
- The fingerprint is `"{type}-{city}-{street ?: ""}-{subCategory}-{TX}"`.
- A listing counts as a duplicate if it has the same id, or the same fingerprint with a size within 5%.
- A duplicate from an already-known provider that isn't cheaper is **silently dropped**.
- On Prague data, the no-street bucket `land-Praha--BUILDING_PLOT-SALE` swallows **35% of all listings**. The result is **56 distinct plots dropped and 43 wrongly attached** (§12.1).

**New scheme.** It applies to LAND only; apartment behaviour stays byte-for-byte identical (verified with golden strings and 8 000 randomised sightings, §12.2).

1. **Land fingerprint = city + transaction + exact area.**
   - Street and subtype are dropped. Street is missing on 29–41% of plots, and the same plot can be classified differently on different portals.
   - Recall on Prague data: **196/197** true cross-portal pairs.
   - An area tolerance only adds false merges.
   ```kotlin
   public fun constructFingerprint(
       buildingType: BuildingType,
       locality: Locality,
       subCategory: String,
       transactionType: TransactionType,
       sizeInM2: Double? = null,
   ): String = when (buildingType) {
       BuildingType.LAND ->
           "land-${locality.city}-${transactionType.name}-${checkNotNull(sizeInM2) { "Land fingerprint needs size" }.roundToInt()}"
       else ->
           "${buildingType.name.lowercase(Locale.FRANCE)}-${locality.city}-${locality.street ?: ""}-$subCategory-${transactionType.name}"
   }
   ```
2. **Within one provider, a different id is never a duplicate.** In Prague, 27 of the 35 same-provider collisions are different plots that happen to have the same area.
3. **A cross-provider merge needs a price and city-part guard.** Prague is one big "city", so 16 of the 175 shared fingerprints cover different plots. That causes **10 wrong merges** without the guard and **0** with it.
   - True pairs differ in price by at most 4.6% and never have conflicting city parts.
   - Every wrong merge had a price gap over 29% or a city-part conflict.
4. **Recognise re-sightings of stored duplicates by id.**
   - Add a nullable `id` to `ApartmentDuplicate` and `ApartmentDuplicateEntity`.
   - Store it in `Apartment.toDuplicate()` (`RealEstateService.kt:162-169`) and in the mappers (`MongoClientApartmentRepository.kt:196-203, 238-245`).
   - Look it up in `findByIdOrFingerprint`.

```kotlin
// RealEstateService
private const val LAND_PRICE_TOLERANCE = 0.15

private suspend fun findOriginalApartment(apartment: Apartment): Apartment? {
    val candidates = apartmentRepository.findByIdOrFingerprint(apartment.id, apartment.fingerprint)
    return candidates.firstOrNull { it.id == apartment.id } ?: when (apartment.mainCategory) {
        // a known duplicate wins over a newer plot with the same area
        BuildingType.LAND -> candidates.firstOrNull { apartment.id in it.duplicates.mapNotNull { d -> d.id } }
            ?: candidates.firstOrNull { it.mainCategory == BuildingType.LAND && it.isSameRealEstateAs(apartment) }
        else -> candidates.firstOrNull { it.fingerprint == apartment.fingerprint && it.isSameRealEstateAs(apartment) }
    }
}

private fun areApartmentsDuplicates(apartment: Apartment, original: Apartment?): Boolean =
    original != null
        && original.transactionType == apartment.transactionType
        && original.mainCategory == apartment.mainCategory
        && (apartment.id == original.id || original.isSameRealEstateAs(apartment))

private fun Apartment.isSameRealEstateAs(other: Apartment): Boolean =
    when (mainCategory) {
        BuildingType.LAND -> other.id in duplicates.mapNotNull { it.id }
            || (other.provider != provider
                && duplicates.none { it.provider == other.provider }
                && hasSimilarPrice(other)
                && !hasConflictingCityPart(other))
        else -> areDoublesEqualWithTolerance(sizeInM2, other.sizeInM2)
    }

private fun Apartment.hasSimilarPrice(other: Apartment): Boolean =
    price <= 0 || other.price <= 0 || areDoublesEqualWithTolerance(price, other.price, LAND_PRICE_TOLERANCE)

private fun Apartment.hasConflictingCityPart(other: Apartment): Boolean {
    val mine = locality.district.toComparableCityPart()
    val theirs = other.locality.district.toComparableCityPart()
    return mine != null && theirs != null && mine != theirs
}
```

```kotlin
// service/.../util/StringUtil.kt
private val PRAGUE_NUMBERED_DISTRICT = Regex("""^Praha( \d+)?$""")
private val COMBINING_MARKS = Regex("""\p{M}+""")

// "Praha-Radotín" / "Radotín" -> "radotin"; "Praha 5" carries no city part -> null
internal fun String?.toComparableCityPart(): String? =
    this?.trim()?.removePrefix("Praha-")
        ?.takeUnless { it.isBlank() || PRAGUE_NUMBERED_DISTRICT.matches(it) }
        ?.let { Normalizer.normalize(it, Normalizer.Form.NFD).replace(COMBINING_MARKS, "").lowercase() }
```

```kotlin
// MongoClientApartmentRepository.findByIdOrFingerprint: add a third $or branch (needs the 006 index, §8)
Filters.eq("duplicates.id", id)
```

**Accepted trade-offs:**
- A plot relisted under a new id on the **same** portal becomes a new document and triggers a notification. Apartments would merge it.
- Two different plots on different portals still merge if they have identical area, prices within 15%, and no known city part. This didn't happen once on the Prague data.

---

## 8. Database: verdict

**No data migration, no backfill, no schema change.**
- `mainCategory`, `subCategory` and `transactionType` are plain strings, and `LAND` already exists in the enum.
- There is no schema validation, no unique index and no text index.
- Apartment fingerprints are unchanged (verified, §12.2), so there is no rewrite like `005`.
- No LAND documents can exist yet, because no code path ever requested LAND.

**One index-only change unit (`006`).**
- It is required for the `duplicates.id` lookup in §7.3. Without an index, that `$or` branch turns every dedup lookup into a collection scan.
- It also adds a type index for search.

```kotlin
@ChangeUnit(id = "006_RealEstateTypeIndexes", order = "006", author = "ivan_havasi")
public class RealEstateTypeIndexes {
    @Execution
    public fun migration(mongoDatabase: MongoDatabase): Unit = runBlocking {
        val collection = mongoDatabase.getCollection(APARTMENT_COLLECTION_NAME)
        collection.createIndex(Indexes.ascending("duplicates.id"), IndexOptions().name(DUPLICATE_ID_INDEX_NAME))
        collection.createIndex(
            Indexes.compoundIndex(Indexes.ascending("mainCategory"), Indexes.ascending("transactionType"), Indexes.descending("updatedAt")),
            IndexOptions().name(TYPE_UPDATED_AT_INDEX_NAME),
        )
    }

    @RollbackExecution
    public fun rollback(mongoDatabase: MongoDatabase): Unit = runBlocking {
        val collection = mongoDatabase.getCollection(APARTMENT_COLLECTION_NAME)
        collection.dropIndex(DUPLICATE_ID_INDEX_NAME)
        collection.dropIndex(TYPE_UPDATED_AT_INDEX_NAME)
    }
}

private const val DUPLICATE_ID_INDEX_NAME = "duplicates.id_1"
private const val TYPE_UPDATED_AT_INDEX_NAME = "mainCategory_transactionType_updatedAt_-1"
```

**If you want zero DB changes, drop the `duplicates.id` lookup.** The only cost: when a duplicate's area or city changes (say the seller corrects the area), its next sighting is stored and notified as a new plot.

**Hard rule** (verified in bson-kotlin 5.2.1 and by a unit test):
- `DataClassCodec` ignores Kotlin default values on decode, so **every new persisted field must be nullable**.
- Old documents then decode with `null`. This also works inside nested lists: old `duplicates[]` entries without `id` decode with `id = null`.
- Unknown fields are skipped, so rolling back is safe.

**Query-side fix, no migration.** An explicit `subTypes: []` currently matches nothing:

```kotlin
Filters.or(
    Filters.not(Filters.exists("filter.subTypes")),
    Filters.size("filter.subTypes", 0),
    Filters.`in`("filter.subTypes", subTypes),
)
```

**Read-only checks to run yourself** (no one connected to the DB during this research):

```js
db.apartments.countDocuments({ mainCategory: "LAND" })                 // expect 0
db.notifications.countDocuments({ "filter.buildingType": "LAND" })     // expect 0
db.notifications.countDocuments({ "filter.subTypes": { $size: 0 } })   // filters that silently never match
```

> ⚠️ The gitignored `application-dev.yaml` points at the production database name and inherits `migrate-at-start: true`. Running `quarkusDev` will apply `006` to prod. An online index build is low-risk, but be aware of it.

---

## 9. REST API

```kotlin
// FindRealEstatesCommand
val subCategories: List<String> = emptyList(),

// RealEstateController.findRealEstates
@DefaultValue("1000000") @QueryParam("sizeMax") sizeMax: Int,   // was 1000, which hides most plots
@QueryParam("subCategory") subCategories: List<String>,         // ?subCategory=BUILDING_PLOT; absent -> empty list (verified)

// MongoClientApartmentRepository.createBaseFilters(): today a single Filters.and(...) expression
private fun FindRealEstatesCommand.createBaseFilters(): Bson {
    val filters = mutableListOf(
        Filters.eq("transactionType", transactionType.name),
        Filters.eq("mainCategory", buildingType.name),
        Filters.gte("sizeInM2", sizeMin.toDouble()),
        Filters.lte("sizeInM2", sizeMax.toDouble()),
        Filters.gte("price", priceMin.toDouble()),
        Filters.lte("price", priceMax.toDouble()),
    )
    if (subCategories.isNotEmpty()) filters.add(Filters.`in`("subCategory", subCategories))
    return Filters.and(filters)
}

// RealEstateController.processNewRealEstates (ADMIN): parametrised for manual land runs
@DefaultValue("APARTMENT") @QueryParam("building") building: String,
@DefaultValue("SALE") @QueryParam("transaction") transaction: String,
@QueryParam("landSubCategory") landSubCategory: String?,

// statistics size cap (MongoClientApartmentRepository.kt:334)
val maxSize = if (command.buildingType == BuildingType.LAND) 1_000_000.0 else 10_000.0
filters.add(Filters.lt("sizeInM2", maxSize))
```

Quarkus REST behaviour (verified in the probe):
- A repeated query param binds as a list. Comma-separated values are **not** split.
- An absent list param arrives as an empty list, never null.
- **Kotlin default values on resource params are ignored**, so a non-null param receives `null`. Every non-null param needs `@DefaultValue`. This already breaks `StatisticsController` (§15.2).

Statistics:
- Price-0 duplicates drag `lowestPrice` and `lowestPricePerM2` down, and this affects apartments too. Wrap `$duplicates.price` and `$duplicates.pricePerM2` in `$filter: {cond: {$gt: ["$$this", 0]}}`.
- Consider making `buildingType` default to `APARTMENT`, so that leaving it out never mixes land into apartment numbers.

---

## 10. Notifications

**Matching works unchanged.** It already compares building type, transaction, size and price ranges, and `subTypes`. Land alerts store `subTypes = ["BUILDING_PLOT"]`. Apply the `[]` fix from §8.

**Price on request (D4) behaves as it does for apartments.** These listings match every filter without `price.from`. On Prague data:

| Filter | Matching plots | Of which price on request |
|---|---|---|
| 500–1500 m², ≤ 15 M | 79 | 17 (22%) |
| 600–1200 m², ≤ 10 M | 28 | 12 (43%) |

**Cross-portal duplicates send a second notification** containing the original plus the duplicate. Apartments already work this way.

**Templates assume apartments:**

| Location | Problem |
|---|---|
| `RestEmailClient.kt:65` | Subject prints a `null` street and hardcodes the currency. |
| `RestEmailClient.kt:69, 92, 148, 152` | "Apartment" headings and alt text. |
| `RestEmailClient.kt:75` | **Template bug:** `$locality.street` prints `Locality(...)` followed by a literal `.street`. |
| `RestEmailClient.kt:76` | Prints `LAND - BUILDING_PLOT`. |
| `RestEmailClient.kt:159` | Prints `null, city, null`. |
| `RestDiscordClient.kt:72-73` | Size has no unit and no thousands separator. |
| `RestDiscordClient.kt:83` | Locality contains nulls. |
| `RestDiscordClient.kt:88` | Prints `LAND - BUILDING_PLOT`. |
| Discord `calculateDiscountPrice` | A price-0 duplicate shows as "Discount Price 0". Ignore duplicates with price ≤ 0. |

Fix with shared helpers used by both clients:

```kotlin
// rest/util
internal fun Apartment.typeLabel(): String =
    when (mainCategory) {
        BuildingType.LAND -> LandSubCategory.fromValueOrNull(subCategory)?.label ?: "Land"
        else -> listOfNotNull(mainCategory.name.firstCapitalOthersLowerCase(), subCategory).joinToString(" ")
    }

internal fun Locality.toDisplayString(): String =
    listOfNotNull(street, district, city).distinct().joinToString(", ")
```

Example subject: *"Building plot 1 200 m² for sale in Radotín, Praha for 3 200 000 CZK"*.

---

## 11. Frontend (`reality-app-fe`)

### 11.1 Design language to match

- **Hero:** purple `.gradient-header` (`#667eea → #764ba2`) with large `Form.Select`/`Form.Control` controls (`size="lg" className="shadow-sm border-0"`, 48 px high).
- **Advanced search:** a glass `Collapse` panel with white labels and `DualRangeSlider`.
- **Cards:** `.real-estate-card` with a purple `.price-badge`, a dark provider badge and `.property-feature` chips.
- **Icons and theme:** Font Awesome 6.4 free, dark mode via `data-bs-theme`, mobile breakpoint at 768 px.
- **Language:** English, kept as is (D6).

### 11.2 Search page (`FE:pages/RealEstates.tsx`)

**Add a type toggle on the existing page, not a new sidebar entry.**
- The page reads the URL only on mount and writes it with `replaceState`, so a sidebar link to `?building=LAND` wouldn't remount it.
- `Sidebar.isActiveRoute` compares only the pathname, so both entries would be highlighted.

```
┌──────────────────────────── gradient-header ──────────────────────────────┐
│ 🌳 Real Estates                                                           │
│ Building plots in Prague                                                  │
│ ┌──────────────┬──────────────┐                                           │
│ │ 🏢 Apartments │ 🌳 Land       │   ← ToggleButtonGroup, active = white/purple
│ └──────────────┴──────────────┘                                           │
│ [ Search real estates…                    ] [ For Sale ▾ ] [ Newest ▾ ]   │
│ [                        + Advanced Search                              ] │
│ ┌ glass ──────────────────────────────────────────────────────────────┐   │
│ │ ▦ Plot area     0 m² ●━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━● 10 000+ m²  │   │
│ │ 💵 Price (CZK)  0    ●━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━● 50M+        │   │
│ └─────────────────────────────────────────────────────────────────────┘   │
└───────────────────────────────────────────────────────────────────────────┘
Mobile (<576px): toggle is full width (2 × flex:1).
```

```tsx
<ToggleButtonGroup type="radio" name="building-type" value={building}
  onChange={handleBuildingChange} className="type-switch mb-3 shadow-sm">
  {ENABLED_BUILDING_TYPES.map(t => (
    <ToggleButton key={t} id={`bt-${t}`} value={t} variant="outline-light" size="lg">
      <i className={`${BUILDING_TYPE_META[t].icon} me-2`}></i>{BUILDING_TYPE_META[t].plural}
    </ToggleButton>
  ))}
</ToggleButtonGroup>
```

```css
/* modern-styles.css */
.type-switch .btn { border-color: rgba(255,255,255,.6); min-width: 140px; font-weight: 500; }
.type-switch .btn-check:checked + .btn { background: #fff; color: #764ba2; border-color: #fff; }
@media (max-width: 576px) { .type-switch { display: flex; width: 100%; } .type-switch .btn { flex: 1; min-width: 0; } }
```

**Subtypes (D2).** Only `BUILDING_PLOT` is scraped, so **show no subtype chips**.
- For LAND, always send `subCategory=BUILDING_PLOT`. The hero subtitle reads "Building plots in Prague".
- Add a chip component only once a second land subtype is enabled.

**Other changes:**
- Remove the Building Type select from Advanced Search.
- **Per-type ranges, kept in config.** Reset them, including the debounced copies, whenever the type or SALE/RENT changes:
  ```ts
  export const SIZE_RANGE: Record<BuildingType, { max: number; step: number }> = {
    APARTMENT: { max: 300, step: 5 }, HOUSE: { max: 1000, step: 10 }, LAND: { max: 10_000, step: 50 },
  };
  export const PRICE_RANGE: Record<BuildingType, Record<TransactionType, { max: number; step: number }>> = {
    APARTMENT: { SALE: { max: 100_000_000, step: 100_000 }, RENT: { max: 250_000, step: 500 } },
    HOUSE:     { SALE: { max: 100_000_000, step: 100_000 }, RENT: { max: 250_000, step: 1_000 } },
    LAND:      { SALE: { max: 50_000_000,  step: 50_000 },  RENT: { max: 100_000, step: 500 } },
  };
  ```
  When the slider is at its top, send an open bound (1 000 000 m² / 2e9 CZK). Prague plots range from 109 to 15 465 m², and prices go up to about 36 M CZK.
- **URL state.** Keep the existing `building` param, e.g. `/real-estates?building=LAND&sizeMax=1500`.
  - Omit params that equal the type's defaults.
  - Initialise state lazily from the URL, so the first fetch is already correct.
- **Fetch race.** On mount the page fetches once with the defaults and again with the URL values, and whichever response arrives last wins. Guard with a sequence-number `useRef` or an `AbortController`.

### 11.3 Card, detail, map

- **Card.** Extract it to `components/RealEstateCard.tsx`. For land:
  ```
  [image]  ┌ 3 450 000 Kč ┐            [SREALITY]
  Prodej stavebního pozemku …
  [▦ 1 250 m²] [🌳 Building plot]
  2 760 Kč/m²                          ← fw-semibold text-primary, land only
  📍 Radotín, Praha
  ```
  Move the price badge out of the `images.length > 0` block. Today, listings without images show no price.
- **Price on request** is displayed exactly like apartments (D4), with no special label.
- **Detail page:**
  - Show "Land · Building plot" instead of `LAND - BUILDING_PLOT` (`RealEstateDetail.tsx:689, 832, 117, 128`).
  - Use a "Plot area" size card, switching to hectares at 10 000 m² and above.
  - Make `formatCurrency(pricePerM2)` null-safe (`:59-64`; call sites `:150, :216, :428, :509, :759`). Land never sends null, but the type allows it and the page crashes on it.
- **Map popup** (`MultiRealEstateMap.tsx:181-183`): show "Building plot · 1 250 m² · 2 760 Kč/m²". **HTML-escape the scraped `name`** (§15.2).

### 11.4 Alerts (`AddNotification.tsx`, `Notifications.tsx`)

- **Building types:** APARTMENT and LAND. **Remove `COMMERCIAL`**, which the backend enum rejects (`AddNotification.tsx:278-280`, `types/notifications.ts:7-13`).
- **Subtypes:**
  - LAND submits `subTypes: ["BUILDING_PLOT"]` automatically, with no subtype UI while there is only one.
  - APARTMENT keeps its current behaviour.
  - Never send `subTypes: []`.
- **Labels:** "Plot area (m²)" when LAND is selected.
- **Notification cards:**
  - Add a summary chip row: type, transaction, subtype label.
  - Fix `renderFilterRange` (`:132-138`), which treats `from: 0` as absent.
  - Delete the dead `renderFilter` (`:140-190`).

### 11.5 Stats (`Stats.tsx`)

- Add the same Apartments | Land toggle, with auto-fetch.
- For LAND, fix the subtype to `BUILDING_PLOT` and hide the "Apartment Layout" select.
- **Reset `subCategory` when the type changes.** Otherwise "2+kk" carries over to LAND and returns nothing.
- Show price per m² in full, e.g. "20 649 Kč/m²" (the Prague median), not "21K".

### 11.6 Types & formatting

```ts
// types/property.ts: single source; notifications.ts and statistics.ts re-export
export type BuildingType = 'APARTMENT' | 'HOUSE' | 'LAND';
export type TransactionType = 'SALE' | 'RENT';
export const ENABLED_BUILDING_TYPES: BuildingType[] = ['APARTMENT', 'LAND'];

// labels identical to backend LandSubCategory.label
export const LAND_SUB_TYPE_LABELS: Record<string, string> = {
  BUILDING_PLOT: 'Building plot', COMMERCIAL: 'Commercial', FIELD: 'Field', MEADOW: 'Meadow', FOREST: 'Forest',
  GARDEN: 'Garden', ORCHARD_VINEYARD: 'Orchard / vineyard', POND: 'Pond', OTHER: 'Other',
};
export const ENABLED_LAND_SUB_TYPES = ['BUILDING_PLOT'];
export const BUILDING_TYPE_META: Record<BuildingType, { label: string; plural: string; icon: string }> = {
  APARTMENT: { label: 'Apartment', plural: 'Apartments', icon: 'fas fa-building' },
  HOUSE:     { label: 'House',     plural: 'Houses',     icon: 'fas fa-house' },
  LAND:      { label: 'Land',      plural: 'Land',       icon: 'fas fa-tree' },
};
```

```ts
// utils/format.ts: replaces the 3 duplicated formatCurrency copies; same output for every type
const cz = (v: number) => Math.round(v).toLocaleString('cs-CZ');
export const formatCurrency = (v: number | null | undefined, cur = 'CZK') =>
  v == null ? '—' : `${cz(v)} ${cur === 'CZK' ? 'Kč' : cur}`;
export const formatPricePerM2 = (v: number | null | undefined, cur = 'CZK') => `${formatCurrency(v, cur)}/m²`;
export const formatArea = (m2: number) =>
  m2 >= 10_000 ? `${(m2 / 10_000).toLocaleString('cs-CZ', { maximumFractionDigits: 2 })} ha` : `${cz(m2)} m²`;
export const subCategoryLabel = (type: string, sub: string | null) =>
  !sub ? '' : type === 'LAND' ? LAND_SUB_TYPE_LABELS[sub] ?? sub : sub;
```

Types and services:
- `types/realEstate.ts`: `pricePerM2: number | null`, `subCategory: string | null`, `mainCategory: BuildingType`, and nullable `district`/`street`.
- `services/RealEstateService.ts`:
  - replace the 10 positional params with an options object;
  - append `subCategory`;
  - **always send `sizeMax` explicitly**;
  - fix the double `encodeURIComponent` on `search` (`:38`).

### 11.7 FE change list

| Priority | File | Change |
|---|---|---|
| P0 | `types/property.ts` (new), `types/*.ts` | Shared types and labels; drop COMMERCIAL/OTHER; fix nullability. |
| P0 | `utils/format.ts` (new) | Null-safe cs-CZ formatting, m²/ha, labels. |
| P0 | `services/RealEstateService.ts` | Options object, `subCategory`, explicit `sizeMax`, encoding fix. |
| P0 | `pages/RealEstates.tsx` + CSS | Type toggle, per-type ranges with reset, lazy URL init, fetch-race guard. |
| P0 | `components/RealEstateCard.tsx` (extracted) | Area and subtype labels, price/m² line, price badge without images. |
| P0 | `pages/RealEstateDetail.tsx`, `components/MultiRealEstateMap.tsx` | Null-safe price/m², labels, plot-area card, popup escaping. |
| P1 | `components/AddNotification.tsx`, `pages/Notifications.tsx` | LAND option with automatic `["BUILDING_PLOT"]`, no `[]`, summary chips, range fix. |
| P1 | `pages/Stats.tsx` | Toggle, subtype reset, full price/m² formatting. |
| P2 | `Dashboard.tsx`, `ReceivedNotifications.tsx`, `Login.tsx` copy | Land quick action; type chip (needs nullable backend fields); generic wording. |

---

## 12. Validation results

### 12.1 Live Prague replay (2026-10-05)

**What was compared.** Every current Prague building plot for sale: Sreality 237, iDNES 217 and Bezrealitky 11 listings.
- These 465 listings are **274 unique plots**. 184 plots appear on 2 portals and 3 appear on all three.
- "Same plot" was established **independently of the fingerprint**, from any of: exact area plus exact price, the same agency, GPS within 150 m, or image dHash.
- 7 clusters remained uncertain and were counted as neither right nor wrong.

**Parsing** (proposed code; jsoup 1.18.3 for the HTML portals):

| | Sreality | iDNES | Bezrealitky |
|---|---|---|---|
| Area correct | 237/237 (= detail `estate_area`) | 189/189 priced (= site's own Kč/m²); 217/217 parsed | 11/11 (= `surfaceLand`) |
| Subtype resolved | 237/237 | 217/217 | 11/11 |
| Price on request | 30 | 27 (+1 decimal-comma bug) | 0 |
| URL works | 6/6 sampled (today's `/byt/` URL: 404) | 6/6 | ✓ |
| Listings outside Prague | 0 | 0 | 0 |

**Dedup simulation.** Sequential, all pages; the outcome was the same for every provider order.

| Scheme | Correct merges | Wrong merges | Distinct plots silently dropped | Missed merges |
|---|---|---|---|---|
| Current (street key + 5%) | 124 | 43 | **56** | 13 |
| New fingerprint + rules 1–2 + 4 | 180 | 10 | 0 | 3 |
| **New + price/city-part guard (§7.3)** | **189** | **0** | **0** | **2** |
| … + ±1 m² area tolerance | 186 | 3 | 0 | 2 |

**Price per m² (Kč):**

| | Min | P10 | Median | P90 | Max |
|---|---|---|---|---|---|
| Sreality | 982 | 10 561 | 20 649 | 40 015 | 105 413 |
| iDNES | 251 (a share) | 8 826 | 20 507 | 39 334 | 105 413 |
| Bezrealitky | 4 174 | 12 896 | 20 877 | 34 783 | 41 475 |

**Coverage:**
- With today's early stop, the first run reaches 46% of Sreality and 58% of iDNES. The full scan (§7.2) reaches 100%.
- Within an hour, 1 genuinely unseen listing appeared, at position 43.

### 12.2 Kotlin prototype: 25/25 unit tests pass

A scratch copy of the backend was used, with fake repositories and providers (no DB, no network). It implemented:
- `LandSubCategory` and the new command field;
- the land fingerprint;
- the nullable `duplicates[].id`, with mappers and the `duplicates.id` lookup;
- land dedup rules 1, 2 and 4 from §7.3;
- `forEachSequentially`;
- the `006` migration.

| Scenario | Result |
|---|---|
| Two plots of 968 and 991 m², same portal and city | Both saved and notified. Current code drops one or misattaches it. |
| Same area, same portal, different ids | Both saved. |
| Same plot on Sreality, then on Bezrealitky | Stored as a duplicate, with its id. |
| That duplicate seen again | Not saved, not notified. |
| A different Bezrealitky plot with the same area | Saved as new. |
| Stored duplicate vs a newer plot with the same area | Stays with its original. This was a design flaw, now fixed. |
| Duplicate whose area changed | Recognised via `duplicates.id`. This was a design flaw, now fixed. |
| Plot on two portals in the same run | Merged when providers run sequentially, stored twice when they run in parallel. Design flaw, now fixed for land. |
| Apartments | Golden fingerprints are identical. 8 000 randomised sightings give **identical decisions** to the old algorithm. |
| Price on request | Saved with `pricePerM2 = 0`. |
| Codec | Old documents without `duplicates[].id` decode. Defaults are confirmed to be ignored. |
| Config | Injecting `List<Enum>` fails; `MutableList` works. |

> **Not in the prototype:** the price/city-part guard from §7.3. It was validated only in the Prague data simulation (§12.1). Add unit tests for it, for example the Satalice vs Modřany pair, both 954 m².

### 12.3 Framework behaviour (Quarkus 3.17.5, verified in bytecode and a probe app)

| Question | Answer |
|---|---|
| Null `@QueryParam` on the REST client | Omitted from the URL ✓ |
| REST client follows redirects | **No**, by default. `quarkus.rest-client."<key>".follow-redirects` turns it on; keep it off. |
| 404 with a `RestResponse<String>` return type | **Throws** `ClientWebApplicationException` before your code sees it. |
| Repeated `@QueryParam List<String>` on the server | Binds ✓. Absent gives an empty list. Commas are not split. |
| Kotlin defaults on resource params | **Ignored.** A non-null param receives `null`. |
| `concurrentExecution = SKIP` on a `suspend fun` | Works. The flag is held until the coroutine completes. |
| Cron `off` / `disabled` | Disables the job ✓. A bare YAML `off` is read as a boolean, and startup fails. |
| Cron `"-"` | **Startup failure.** |
| `List<Enum>` `@ConfigProperty` (Kotlin) | **Breaks bean creation.** Use `MutableList`. |
| bson-kotlin data class decode | Ignores defaults and skips unknown fields. Nullable new fields are safe. |

---

## 13. Testing plan

Start from the prototype's tests (§12.2), then add:

1. **Unit, fingerprint:** apartment golden strings are unchanged; land uses city + transaction + area.
2. **Unit, dedup:** all §12.2 scenarios, plus the **price/city-part guard** with real Prague cases:
   - Satalice vs Modřany, both 954 m²: not merged.
   - Kyje vs Modřany, both 890 m²: not merged.
   - A true pair 1.3% apart in price: merged.
   - One side with a city part and the other without: merged.
   - `toComparableCityPart` cases: "Praha-Radotín", "Praha 5", diacritics.
3. **Unit, providers, on saved fixtures:**
   - Sreality: a "za m²" plot, a price-0 plot, a "za m²" price above 60 k, URL building.
   - Bezrealitky: a land card without a street; a per-m² suspect gets skipped; the 307 page yields an empty list.
   - iDNES: "Prodej pole 24 066 m²", "Prodej zahrady 295 m²", "Cena na vyžádání", **"17 999 999,44 Kč"**, apartment "2+1 65 m²", and a 404 yielding an empty list.

   The research fixtures are under `/Users/ivanhavasi/.claude/jobs/cb36793c/tmp/prague/raw/` and `.../permtwo/`. **Copy them into `src/test/resources`, because the job directory is temporary.**
4. **Unit, fetch loop:** LAND scans every page and stops at the first empty fetch; providers run sequentially; apartments still stop early.
5. **IT, `findAll`:** LAND and APARTMENT are kept apart; the `subCategories` filter works; plots over 1 000 m² are returned under the new default.
6. **IT, notification matching:** LAND + `[BUILDING_PLOT]` matches; an APARTMENT filter doesn't match LAND; `[]` matches everything after the fix.
7. **Unit, `rest`:** land email and Discord output has no `null`, and uses the labels and units.

> ⚠️ Gradle 8.9 does not run on JDK 25. Set `JAVA_HOME` to JDK 21. Read §15.2 #2 before running any `*IT`.

---

## 14. Rollout plan

1. **Fix the security bug** (§15.2 #1). It is independent and small.
2. **Backend PR 1, deployed with `land-cron` off** (no effect on prod):
   - `LandSubCategory` and the new command field
   - the three provider changes, including the iDNES decimal-comma fix
   - the new fingerprint, dedup and guard, plus nullable `duplicates[].id`
   - sequential providers and the full scan for land
   - the land scheduler job
   - the parametrised `/process` endpoint
   - the `006` index
   - tests
3. **Deploy, then trigger a manual run:**
   ```
   POST /api/real-estates/process?building=LAND&transaction=SALE&landSubCategory=BUILDING_PLOT
   ```
   Check that:
   - about 274 unique plots are stored, with about 190 cross-portal duplicates attached;
   - no size is 1 or 0;
   - prices are totals;
   - URLs open;
   - nothing says "Unknown …";
   - the logs show the expected skips.

   No LAND alerts exist yet, so nobody is notified.
4. **Enable `LAND_SCHEDULER_CRON`.** Watch the parse and skip warnings for a few days.
5. **Backend PR 2:**
   - the `subCategory` search filter
   - the new `sizeMax` default
   - the statistics size cap and the price-0 duplicate filter
   - the notification template helpers
   - the `subTypes: []` fix
6. **FE PR 1:** search, cards, detail page and map (P0).
7. **FE PR 2:** alerts and stats (P1). Users can create LAND alerts only from this point, and because the full scan has already run, they are notified only about genuinely new plots.

---

## 15. Problems & risks

### 15.1 Land-specific

| # | Risk | Status / mitigation |
|---|---|---|
| 1 | False merges silently drop plots | Solved by §7.3 (0 wrong on Prague data). |
| 2 | Per-m² prices | Sreality: fixed via `price_summary_czk` (10/10). Bezrealitky: detect and skip (§6.4). |
| 3 | Price on request is 12.4% of Prague plots and matches alerts without `price.from` | Accepted (D4), same as apartments. |
| 4 | Bezrealitky results never carry the land type | Always fetch per subtype; an unfiltered LAND fetch returns an empty list. |
| 5 | Sreality silently ignores invalid sub codes | Codes come only from a typed enum. |
| 6 | Pagination: iDNES returns 302 then 404; Bezrealitky returns 307 to a generic feed | Treat these as end of results, and never enable follow-redirects (§6.2, §6.3). |
| 7 | New listings appear past page 0 because portals order by bump | Land always scans every page (§7.2). |
| 8 | A plot listed on two portals in the same run is stored twice | Land providers run sequentially (§7.2). Apartments keep the issue (§15.2). |
| 9 | iDNES fractional shares look like normal plots (~2%) | Accepted in phase 1; the per-m² rule catches the cheap ones (§16). |
| 10 | Bezrealitky's hashed CSS classes change when the site redeploys | All 5 still match. Long term: parse `__NEXT_DATA__`. |
| 11 | Size caps hide land: API 1000, FE 300, stats < 10 000 | §9, §11.2 |
| 12 | Config traps that can stop all scraping | Avoided in §7.1. |
| 13 | HOUSE has the same Sreality bugs | Apply the same pattern when adding houses. |

### 15.2 Pre-existing problems found (not caused by land)

Ordered by severity. **(V)** means I confirmed it in code or with the probe. The rest were reported by the research agents with file:line references.

1. **Security (V): the notification ownership check is bypassed for every normal user.**
   - `RequireUserMatchFilter.kt:29-31` reads `if (identity.hasRole(USER_ROLE)) return // skip if the user has admin role`. It checks **USER**, not ADMIN, and every Google login gets USER.
   - So any logged-in user can list, add, delete, enable or disable anyone's notifications, including email addresses and Discord webhook URLs.
   - Delete, enable and disable also act on `notificationId` alone, with no owner check (`MongoClientUserNotificationRepository.kt:44-57`).
2. **Integration tests may wipe a real `apartments` collection.**
   - `AbstractIT.cleanUp()` deletes everything (V), and the test DB and collection names are the same as prod (`reality`/`apartments`).
   - If `MONGODB_CONNECTION_STRING` is set in your shell, Dev Services are skipped and the tests run against that database.
   - Unset the variable before running tests, or give tests their own database name.
3. **`quarkusDev` runs migrations against prod (V)** (§8).
4. **XSS from scraped data:** in the Leaflet popup (`FE:components/MultiRealEstateMap.tsx:165-194`) and in the email HTML (`RestEmailClient.kt:163`).
5. **`StatisticsController` (V via probe).**
   - The Kotlin defaults for `period` and `granularity` are ignored, so the endpoint NPEs when a client omits them. The FE currently always sends them.
   - The endpoint is public (no `@RolesAllowed`).
   - `$week` + `$year` puts weeks in the wrong bucket around New Year.
6. **Dev cron `"-"` (V):** Quarkus fails to start with it in the probe. Use `"off"`.
7. **iDNES decimal-comma prices parse as 0**, for apartments too (§6.3).
8. **Sreality RENT apartment URLs use `/prodej/`** (`:108-109`). The portal may redirect; unverified for apartments.
9. **Email template bug** at `RestEmailClient.kt:75`, and `null`s printed in subjects and localities.
10. **One failing Discord webhook stops the rest** (`DiscordWebhookNotificationEventHandler.kt:45-47`). Descriptions over 4 096 characters also fail.
11. **Apartments: cross-portal pairs in the same run can be stored twice.** Providers run in parallel, and there is no unique index on `externalId`. Overlapping scheduler runs made this worse; `SKIP` (§7.1) fixes the overlap.
12. **A price drop on a listing is stored as a duplicate of itself**, and the main document's price never updates (`RealEstateService.kt:136-141`).
13. **Statistics:** price-0 duplicates lower `lowestPrice` and `lowestPricePerM2` (§9).
14. **DB name defined twice:** `DatabaseNames.DB_NAME = "reality"` is hardcoded, while Mongock uses `${MONGODB_DATABASE}`.
15. **`CLAUDE.md` is out of date:**
    - the cron runs every 30 minutes, not 20;
    - `SentNotificationRepository` is never consulted before sending;
    - there is a redundant ascending/descending `updatedAt` index pair.
16. **FE:**
    - `search` is URL-encoded twice;
    - the "debounce" doesn't debounce;
    - changing the search doesn't reset the page;
    - reset sets values outside the slider limits;
    - `fa-bell-plus` and `fa-calendar-edit` render blank (they're not in FA 6 free);
    - Received Notifications' "Load More" replaces the list instead of appending.
17. **Scrapers:**
    - Bezrealitky's id is the URL slug, which may be unstable.
    - Bezrealitky's card description is always null.
    - Bezrealitky's headline is missing a space ("Prodej pozemkuU Plynárny").
    - iDNES logs a page past the end as an error.

---

## 16. Remaining open items

| # | Item | Recommendation |
|---|---|---|
| 1 | Bezrealitky per-m² prices: skip (D5), or correct when the description confirms? | Skip in phase 1, and add correction if it ever matters (§6.4). |
| 2 | iDNES fractional shares (~2% of Prague listings) | Accept them. Optionally fetch the detail title ("podíl") for new iDNES land, or filter by `data-brand` for known share-selling agencies. |
| 3 | `006` index vs. zero DB changes | Add `006`: it only creates indexes and builds online. |
| 4 | Beyond Prague (future) | Region params: Sreality `locality_region_id=10,11`; iDNES `praha-zapad`, `praha-vychod`, `stredocesky-kraj`; Bezrealitky `R442397`. The iDNES and Bezrealitky locality parsers must extract the municipality. A full scan per run would grow to about 100+ pages, so early-stop tuning would be needed. |

---

## Appendix A: verified URLs

```
# Sreality (JSON)
https://www.sreality.cz/api/v1/estates/search?category_type_cb=1&category_main_cb=3&category_sub_cb=19&locality_country_id=112&locality_region_id=10&limit=22&offset=0&lang=cs
https://www.sreality.cz/api/v1/estates/{hash_id}                         # detail: estate_area, utilities, description
https://www.sreality.cz/detail/prodej/pozemek/bydleni/{locality}/{hash_id}

# Bezrealitky (HTML)
https://www.bezrealitky.cz/vyhledat?offerType=PRODEJ&estateType=POZEMEK&landType=STAVEBNI&regionOsmIds=R435514&currency=CZK&location=exact&page=1
https://www.bezrealitky.cz/nemovitosti-byty-domy/{id}-nabidka-prodej-pozemku-{street}-{city}
POST https://api.bezrealitky.cz/graphql/                                 # unofficial; has descriptions in bulk

# iDNES (HTML)
https://reality.idnes.cz/s/prodej/pozemky/stavebni-pozemek/praha/?page=N   # 0-based; 302 then 404 past the end
https://reality.idnes.cz/detail/prodej/pozemek/{locality-slug}/{24-hex-id}/
```

## Appendix B: sample payloads (trimmed)

**Sreality: plot priced per m²**

```json
{
  "hash_id": 1140731980,
  "advert_name": "Prodej stavebního pozemku 6329 m²",
  "category_sub_cb": {"name": "Bydlení", "value": 19},
  "price_czk": 1990.0, "price_czk_m2": 1990, "price_summary_czk": 12594710.0,
  "price_unit_cb": {"name": "za m²", "value": 3},
  "locality": {"city": "Frýdek-Místek", "citypart": "Skalice", "street": "", "gps_lat": 49.6537616, "gps_lon": 18.4033505}
}
```

**Bezrealitky: `__NEXT_DATA__` apolloCache entry**

```json
"Advert:1061879": {
  "uri": "1061879-nabidka-prodej-pozemku-u-plynarny-praha",
  "estateType": "POZEMEK", "landType": "UNDEFINED",
  "address({\"locale\":\"CS\"})": "U Plynárny, Praha - Michle",
  "surface": 0, "surfaceLand": 217, "price": 8999999, "currency": "CZK",
  "gps": {"lat": 50.0555, "lng": 14.4565}
}
```

**iDNES: land card**

```html
<div class="c-products__inner">
  <a href="https://reality.idnes.cz/detail/prodej/pozemek/praha-8-sporicka/6a1070ded3d20e1db406c2c5/" class="c-products__link" data-brand="NEXT REALITY GROUP a.s.">
    <span class="c-products__img"><img data-src="https://sta-reality2.1gr.cz/.../84c83db6ca93c077671548ed0e1be.webp"></span>
    <h2 class="c-products__title"><span class="text-capitalize">prodej</span> stavebního pozemku 473&nbsp;m²</h2>
    <p class="c-products__info">Spořická, Praha 8 - Dolní Chabry</p>
    <p class="c-products__price"><strong>11 500 000 Kč</strong> <span>(24 313 Kč/m²)</span></p>
  </a>
</div>
```

## Appendix C: research artifacts (temporary)

These live in the job's temp directory, **which is deleted with the job**. Copy anything you want to keep.

| What | Path |
|---|---|
| Kotlin prototype (scratch repo copy) | `/Users/ivanhavasi/.claude/jobs/cb36793c/tmp/scratch/reality-app` |
| Prototype patch of tracked files | `/Users/ivanhavasi/.claude/jobs/cb36793c/tmp/scratch/land-prototype-tracked.patch` |
| Prague replay: raw data, jsoup parser, dedup simulation (`run_all.sh`) | `/Users/ivanhavasi/.claude/jobs/cb36793c/tmp/prague/` |
| Per-m² research: GraphQL schema, CZ datasets, rule evaluation | `/Users/ivanhavasi/.claude/jobs/cb36793c/tmp/permtwo/` |
| Quarkus probe app | `/Users/ivanhavasi/.claude/jobs/cb36793c/tmp/scratch/quarkus-probe` |
