# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a real estate aggregator application built with Kotlin and Quarkus that collects, processes, and serves property listings from multiple Czech real estate portals (Sreality, Bezrealitky, iDNES Reality). The application scrapes listings, deduplicates them, stores them in MongoDB, and notifies users about new listings via email, Discord, or custom webhooks.

## Build Commands

```bash
# Build the entire project
./gradlew build

# Run the application (Quarkus dev mode)
./gradlew quarkusDev

# Run unit tests (fakes only, no DB or network)
./gradlew :service:test :rest:test :sreality:test :bezrealitky:test :idnes:test

# Integration tests (*IT, application module) wipe the `apartments` collection of the `reality` database.
# If MONGODB_CONNECTION_STRING is set, Dev Services are skipped and they run against that database: unset it first.
./gradlew :application:test

# Clean build
./gradlew clean build
```

## Architecture

The application follows **hexagonal architecture** with clear separation between modules:

### Module Structure

- **`model/`** - Shared domain models and commands
  - Contains core entities: `Apartment`, `User`, `Notification`, `SentNotification`
  - Command pattern for operations (e.g., `GetRealEstatesCommand`, `SendNotificationsCommand`)
  - Enums for types: `ProviderType`, `NotificationType`, `BuildingType`, `TransactionType`

- **`service/`** - Core business logic
  - `RealEstateService`: Main orchestrator for fetching and saving apartments
  - `UserNotificationService`: Handles notification logic
  - Provider pattern: `RealEstatesProvider` interface for extensibility
  - Repository interfaces for data access (implementations in `mongo/`)

- **`rest/`** - REST API layer
  - Controllers: `RealEstateController`, `UserController`
  - External API clients: `DiscordApi`, `MailjetApi` (using Quarkus REST Client)
  - Security: Google OIDC authentication with role-based access control

- **`mongo/`** - MongoDB persistence
  - Entities with custom codecs for type conversions
  - Repository implementations using MongoDB reactive driver with Kotlin coroutines
  - Mongock migrations in `migration/` directory (note: `004` creates a redundant ascending/descending `updatedAt` index pair)

- **`sreality/`, `bezrealitky/`, `idnes/`** - Provider-specific implementations
  - Each implements `RealEstatesProvider` interface
  - Sreality uses REST API, Bezrealitky/iDNES use HTML scraping (Jsoup)
  - Convert provider-specific models to common `Apartment` model

- **`application/`** - Main entry point
  - Aggregates all modules
  - Contains `application.yaml` configuration
  - Minimal code - just a marker class for Quarkus to bootstrap

### Key Architectural Patterns

**Provider Pattern**: Adding a new real estate portal requires:
1. Create a new module (e.g., `newportal/`)
2. Implement `RealEstatesProvider` interface
3. Mark implementation as `@ApplicationScoped` (auto-discovered by CDI)
4. Add module to `application/build.gradle.kts`

**Duplicate Detection** (`RealEstateService.filterApartments()`, fingerprint in `service/util/FingerprintUtil.kt`):
- Candidates come from `findByIdOrFingerprint`: same `externalId`, same fingerprint, or a stored `duplicates.id`
- **Apartments**: fingerprint `"{type}-{city}-{street}-{subCategory}-{TX}"`; a listing is a duplicate if it has the same id, or the same fingerprint with a size within 5%
- **Land** (`BuildingType.LAND`, `subCategory` = `LandSubCategory.name`): fingerprint `"land-{city}-{TX}-{area}"` (street and subtype dropped: often missing, and portals classify plots differently). A listing is a duplicate if it has the same id, is a known duplicate (`duplicates.id`), or is the same area **on another portal** with a price within 15% and no conflicting city part (`locality.district`, compared via `toComparableCityPart`). Within one portal a different id is never a duplicate
- A duplicate is stored in the original's `duplicates` array (with its `id`) when it comes from a new portal or is cheaper; otherwise it is dropped
- New persisted fields must be nullable: the bson-kotlin codec ignores Kotlin defaults on decode

**Scheduled Scraping**: `RealityScheduler` has two jobs sharing a mutex, both with `SKIP` concurrency:
- Apartments (`reality.scheduler.cron`, every 30 minutes 6-23h): SALE, then RENT; providers in parallel; up to 5 pages (22 items) per provider, stopping at the first page with nothing new
- Land (`reality.scheduler.land-cron`, env `LAND_SCHEDULER_CRON`, default `"off"`): Prague building plots for sale; providers one after another (so a plot on two portals in one run is merged); every page is scanned (up to 12) because portals reorder by "bump"
- Random delays between requests (700-2500ms) to avoid detection
- Cron values must be quoted; only `"off"`/`"disabled"` disable a job (a bare YAML `off` or `"-"` fails startup)

**Notification System**: Event-driven architecture:
- When new apartments are saved, `UserNotificationService` finds matching user filters
- Handlers: `EmailNotificationEventHandler`, `DiscordWebhookNotificationEventHandler`, `WebhookNotificationEventHandler`
- Sent notifications are recorded via `SentNotificationRepository` (for history only; it is not consulted before sending)

## Configuration

Environment variables required (defined in `application.yaml`):
- `MONGODB_CONNECTION_STRING` - MongoDB connection string
- `MONGODB_DATABASE` - Database name
- `GOOGLE_CLIENT_ID` - Google OIDC client ID for authentication
- `MAILJET_USERNAME`, `MAILJET_PASSWORD` - Email service credentials

REST clients configured for external services:
- Sreality API: `https://www.sreality.cz`
- Bezrealitky: `https://www.bezrealitky.cz`
- iDNES Reality: `https://reality.idnes.cz`
- Discord webhooks: `https://discord.com`
- Mailjet email: `https://api.mailjet.com`

Observability: OpenTelemetry configured to export to Grafana Alloy (endpoint: `http://grafana-alloy:4317`)

## Tech Stack

- **Language**: Kotlin 2.0.21 with JVM 21, explicit API mode enabled
- **Framework**: Quarkus 3.17.5
- **Build**: Gradle with Kotlin DSL
- **Database**: MongoDB with reactive driver + Mongock migrations
- **Async**: Kotlin coroutines throughout (suspend functions)
- **REST**: JAX-RS with Quarkus REST Client (formerly RESTEasy Reactive)
- **HTML Parsing**: Jsoup (for Bezrealitky, iDNES)
- **Auth**: Quarkus OIDC with Google provider
- **Testing**: JUnit 5, Testcontainers, kotlinx-coroutines-test

## Important Notes

- All subprojects enforce **explicit API mode** - public APIs must be marked with `public`/`internal`
- Coroutines are used extensively - most service methods are `suspend fun`
- MongoDB operations use reactive driver adapted with Kotlin coroutines (not blocking)
- Security: `/api/real-estates/process` (POST, `?building=&transaction=&landSubCategory=`) requires ADMIN role, GET endpoints require USER role except `/api/real-estates/{id}` and `/api/statistics/market` (public)
- `@RequireUserMatch` endpoints allow only the user whose id is in the path (ADMIN bypasses); notification repository operations are scoped by user id
- Kotlin default values on JAX-RS resource params are ignored: every non-null query param needs `@DefaultValue`
- Schedulers are configured via `reality.scheduler.cron` and `reality.scheduler.land-cron`
- CORS configured for `https://reality.havasi.me` origin