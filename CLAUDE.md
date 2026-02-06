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

# Run tests
./gradlew test

# Run tests for a specific module
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
  - Mongock migrations in `migration/` directory

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

**Duplicate Detection**: Apartments are identified by:
- **ID**: Portal-specific identifier
- **Fingerprint**: Computed from `buildingType`, `locality`, `subCategory`, `transactionType`
- Logic in `RealEstateService.filterApartments()` finds duplicates across portals
- Cheaper listings from same apartment are saved as `duplicates` array

**Scheduled Scraping**: `RealityScheduler` runs every 20 minutes (6-23h) with:
- Random delays between requests (700-2500ms) to avoid detection
- Processes SALE, then RENT transactions
- Fetches ~5 pages (22 items each) per provider per run

**Notification System**: Event-driven architecture:
- When new apartments are saved, `UserNotificationService` finds matching user filters
- Handlers: `EmailNotificationEventHandler`, `DiscordWebhookNotificationEventHandler`, `WebhookNotificationEventHandler`
- Prevents duplicate notifications via `SentNotificationRepository`

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
- Security: `/api/real-estates/process` (POST) requires ADMIN role, GET endpoints require USER role except `/api/real-estates/{id}` (public)
- Scheduler can be disabled/configured via `reality.scheduler.cron` property
- CORS configured for `https://reality.havasi.me` origin