# Deployment notes

## API hosting
The backend is a stateless FastAPI service. A Render Blueprint is provided in `backend/render.yaml`. Create the service from the repository, then set `FIRMS_MAP_KEY` in the hosting dashboard. Obtain the key from NASA FIRMS: https://firms.modaps.eosdis.nasa.gov/api/area/ . Never commit the real key.

After deployment, verify `/health`, `/docs`, and one bounded `/api/v1/fires` request. Then replace the Android release API URL with the HTTPS service origin (include trailing slash). Keep the debug emulator URL separate.

## API abuse controls
The API uses a process-local sliding-window rate limit (default 120 requests per client IP per minute; configure with `FIREPULSE_RATE_LIMIT_PER_MINUTE`), a maximum bounding-box degree-area (default 5,000; `FIREPULSE_MAX_BBOX_DEGREE_AREA`), and bounded concurrent NASA FIRMS requests (default 4; `FIREPULSE_MAX_UPSTREAM_CONCURRENCY`). Rate-limited requests receive HTTP 429 and `Retry-After`. Oversized and antimeridian-crossing bounding boxes are rejected.

The limiter uses the direct socket peer address and deliberately does not trust `X-Forwarded-For`, which clients can spoof unless a trusted proxy chain is configured. Behind a reverse proxy, configure trusted-proxy behavior before relying on proxy-derived addresses. The limiter and semaphore are per process; they do not enforce global quotas across multiple instances. Use a shared Redis/API-gateway limiter and shared concurrency controls when horizontally scaling.

CORS is disabled by default for the Android/native-only client. For a browser client, set `FIREPULSE_CORS_ORIGINS` to a comma-separated allowlist of exact origins.

## Android background refresh
The app registers one unique WorkManager periodic request on launch. It refreshes the default India bounding box (68, 6, 98, 37.5), VIIRS SNPP near-real-time feed, and five-day window every six hours when network connectivity is available. WorkManager execution is inexact and may be deferred by Android battery optimization and system scheduling. Failures use exponential retry backoff. The worker updates the Room snapshot read by Explore and prunes local cache rows older than 30 days. This is best-effort data freshness, not real-time monitoring or alerting.

## Remaining production security
The prototype still has no user authentication, per-account quotas, distributed/shared rate limiting, persistent/shared cache, comprehensive observability, or formal incident response. Add controls appropriate to the expected audience and hosting topology before public launch. Keep `FIRMS_MAP_KEY` server-side and avoid logging upstream URLs containing the key. Never describe satellite hotspot observations as emergency alerts.

## Android build and verification
Open `android/` in Android Studio and allow Gradle sync. No CI workflow is required. A local build can be performed later with Android Studio or `./gradlew :app:assembleDebug` after a Gradle wrapper is generated. The current branch has not been Gradle-synced, built, or tested on a physical device. Backend tests have been authored but not executed in this environment; source changes remain unverified until tested.
