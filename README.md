# Real Estate Aggregator

A comprehensive real estate aggregation application that collects, processes, and serves property listings from multiple Czech real estate portals.

## Tech Stack

- **Backend**: Kotlin with coroutines
- **Framework**: Quarkus
- **Build System**: Gradle with Kotlin DSL
- **Database**: MongoDB
- **Containerization**: Docker
- **Cloud Infrastructure**: AWS
- **Observability**: OpenTelemetry with Grafana

## Features

- Aggregates real estate listings from multiple Czech portals:
  - Sreality
  - Bezrealitky
  - iDNES Reality
- Unified data model for property information
- RESTful API for accessing aggregated data
- MongoDB-based storage for property listings
- Modular architecture for easy addition of new data sources
- Notifies user about new listings via email, custom webhook, or Discord message

## Project Structure

The application follows hexagonal architecture principles, with a clear separation of concerns. The main modules are:

- `application/` - Main application module
- `model/` - Shared data models
- `mongo/` - MongoDB integration
- `rest/` - REST API endpoints
- `service/` - Core business logic and services
- `sreality/` - Integration with Sreality portal
- `bezrealitky/` - Integration with Bezrealitky portal
- `idnes/` - Integration with iDNES Reality portal
