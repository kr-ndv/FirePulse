# FirePulse architecture

FirePulse consists of a native Android client and a stateless FastAPI service. The service owns the NASA FIRMS MAP_KEY and normalizes MODIS/VIIRS active-fire CSV observations.

## Components
- Android: Kotlin, Jetpack Compose, Material 3 and Retrofit API client.
- API: FastAPI endpoints for observations, GeoJSON and aggregate summaries.
- Source: NASA FIRMS Area API; supported near-real-time MODIS and VIIRS feeds.
- Harmonization: stable observation fields, source/satellite identity, daily counts and prototype spatial grid helper.
- Deployment: container-compatible FastAPI service with a Render Blueprint.

## Data flow
1. Android requests a bounded WGS84 area and date window from FirePulse.
2. Backend validates parameters and reads `FIRMS_MAP_KEY` from its environment.
3. Backend calls NASA FIRMS, parses CSV and normalizes numeric values.
4. The API returns observations, GeoJSON features or a summary aggregation.
5. Android can render detections and explain the sensor/source metadata.

## Trust boundaries and limitations
The MAP_KEY must never be embedded in Android or committed. Current prototype endpoints do not include user authentication, quotas or persistent response caching. Add these before public production use. Current bounding-box requests do not support antimeridian-crossing bounds. Satellite detections are thermal anomalies, not confirmed incidents, perimeters or evacuation guidance.

## Verification status
Source changes are committed on the feature branch. A local Android Gradle sync/build and end-to-end request against a valid FIRMS key have not yet been run; do not treat this repository as build-verified.
