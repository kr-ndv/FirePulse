# Deployment notes

## API hosting
The backend is a stateless FastAPI service. A Render Blueprint is provided in `backend/render.yaml`. Create the service from the repository, then set `FIRMS_MAP_KEY` in the hosting dashboard. The key is obtained from NASA FIRMS: https://firms.modaps.eosdis.nasa.gov/api/area/ . Do not commit the real key.

After deployment, verify `/health`, `/docs`, and one bounded `/api/v1/fires` request. Then replace the Android release API URL with the HTTPS service origin (include trailing slash). Keep the debug emulator URL separate.

## Android background refresh
The app registers one unique WorkManager periodic request on launch. It refreshes the default India bounding box (68, 6, 98, 37.5), VIIRS SNPP near-real-time feed, and five-day window every six hours when network connectivity is available. WorkManager periodic execution is inexact and may be deferred by Android battery optimization and system scheduling. Failures use exponential retry backoff. The worker updates the same Room snapshot read by Explore and prunes local cache rows older than 30 days. This is a best-effort data freshness feature, not a real-time monitoring or alerting system.

## Security before public launch
This prototype has no user authentication, per-client request quotas, persistent/shared cache, or comprehensive abuse controls. The in-memory cache is per backend process and does not coordinate across multiple instances. Before exposing it to untrusted public traffic, add a distributed rate limiter or gateway quotas, bounded upstream concurrency, request-size and geographic-area limits, monitoring, and an authentication strategy appropriate to the client. Restrict CORS to actual web origins if a web client is added. Keep `FIRMS_MAP_KEY` server-side and avoid logging upstream URLs containing the key. Do not describe satellite hotspot observations as emergency alerts.

## Android build and verification
Open the `android/` directory in Android Studio and allow Gradle sync. No CI workflow is required. A local build can be performed later with Android Studio or `./gradlew :app:assembleDebug` after a Gradle wrapper is generated. The current branch has not yet been Gradle-synced, built, or tested on a physical device; treat source implementation as unverified until those checks are completed.
