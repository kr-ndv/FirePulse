# FirePulse architecture

FirePulse consists of a native Android client and a stateless FastAPI service. The service owns the NASA FIRMS MAP_KEY and normalizes MODIS/VIIRS active-fire CSV observations.

## Components
- Android: Kotlin, Jetpack Compose, Material 3, Retrofit API client, MapLibre Native OpenGL map rendering and Room persistence for offline observation snapshots.\n- Map: OpenFreeMap Liberty style with OpenStreetMap attribution; FIRMS detections are rendered as GeoJSON circle features.
- API: FastAPI endpoints for observations, GeoJSON and aggregate summaries.
- Source: NASA FIRMS Area API; supported near-real-time MODIS and VIIRS feeds.\n- API cache: 60-second in-memory cache (up to 128 area/source/window keys) to reduce duplicate FIRMS requests.
- Harmonization: stable observation fields, source/satellite identity, daily counts and prototype spatial grid helper.
- Deployment: container-compatible FastAPI service with a Render Blueprint.

## Data flow
1. Android requests a bounded WGS84 area and date window from FirePulse.
2. Backend validates parameters and reads `FIRMS_MAP_KEY` from its environment.
3. Backend calls NASA FIRMS, parses CSV and normalizes numeric values.
4. The API returns observations, GeoJSON features or a summary aggregation.
5. Android renders detections on the map and explains sensor/source metadata. Successful observation responses are stored in a local Room database, scoped by source and requested day window, and retained for up to 30 days.

## Trust boundaries and limitations
The MAP_KEY must never be embedded in Android or committed. Current prototype endpoints do not include user authentication or quotas. The response cache is process-local and short-lived, not a persistent/shared cache. Add these before public production use. Current bounding-box requests do not support antimeridian-crossing bounds. Satellite detections are thermal anomalies, not confirmed incidents, perimeters or evacuation guidance.

## Verification status
Source changes are committed on the feature branch. A local Android Gradle sync/build and end-to-end request against a valid FIRMS key have not yet been run; do not treat this repository as build-verified.
