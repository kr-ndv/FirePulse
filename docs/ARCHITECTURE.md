# FirePulse architecture

FirePulse is a native Android application built with Kotlin and Jetpack Compose, backed by a companion FastAPI service. The backend owns the NASA FIRMS credential and retrieves external satellite observations.

## Components
- Android: Compose UI, Retrofit client; Room persistence and WorkManager refresh are planned.
- API: FastAPI routes, FIRMS client, validation and processing.
- Source: NASA FIRMS active fire CSV Area API.
- Harmonization: normalized observation schema, UTC date grouping and spatial aggregation while retaining sensor identity.

## Trust boundaries
The FIRMS MAP_KEY is a backend secret. It must never be embedded in the Android application or committed to source control. The Android client communicates with the FirePulse API.

## Current status
The repository foundation includes a single Android Gradle module, initial Compose destinations, Retrofit API contract and FastAPI FIRMS integration skeleton. Map rendering, persistent caching, complete harmonization, tests and production deployment remain subsequent tasks.
