# Deployment notes

## Deploy the FastAPI backend to Vercel

FirePulse includes a Vercel-compatible FastAPI entrypoint at `backend/index.py`. Vercel's Python runtime detects the exported FastAPI `app` and deploys the backend as a Function. The project is pinned to Python 3.12 through `backend/.python-version`.

1. Sign in to Vercel and choose **Add New → Project**.
2. Import the GitHub repository `kr-ndv/FirePulse`.
3. Select the `feature/android-foundation` branch while testing this implementation. Do not change or merge `main`.
4. In Project Settings / Configure Project, set **Root Directory** to `backend`. This is essential: the FastAPI entrypoint and `requirements.txt` are inside that directory.
5. Leave the Framework Preset as **Other** or accept Vercel's FastAPI/Python detection. Do not add a custom Build Command or Output Directory.
6. Add the environment variable `FIRMS_MAP_KEY` with the NASA FIRMS MAP_KEY value. Add it to the Preview environment for branch testing and Production only when ready. Do not put the key in GitHub, Android source, or a client-side variable.
7. Deploy the project. Vercel may build automatically when the Git repository is connected; this is Vercel's deployment build, not a GitHub Actions workflow.

After deployment, copy the generated HTTPS deployment URL and verify:
- `/health` returns a JSON status response.
- `/docs` opens the FastAPI Swagger UI.
- A bounded request to `/api/v1/fires` returns observations when `FIRMS_MAP_KEY` is configured.

Only after the API is deployed and verified should the Android release API base URL be changed from its placeholder to the actual HTTPS URL (with trailing slash). Keep the debug emulator URL separate.

### Vercel runtime considerations

Vercel runs this FastAPI application as a serverless Function, not as a continuously running Uvicorn server. Function instances can start, stop, and scale independently. FirePulse's in-memory rate limiter, semaphore, and response cache are therefore per-instance and may reset or differ between instances; they are not global quotas or durable storage. The Android Room cache remains the app's offline cache.

The NASA FIRMS request timeout and Vercel Function duration must be compatible. If a request times out in deployment, inspect Vercel Function logs and reduce the requested workload or adjust supported function duration within the selected plan's limits. Do not claim that Vercel deployment is verified until the live endpoints have been checked.

Vercel's Hobby plan is free for personal, non-commercial projects, but plan eligibility, usage limits, signup verification and payment checks are controlled by Vercel. This repository configuration cannot guarantee a no-card signup. Do not enter payment details if Vercel asks and you do not wish to provide them.

## Alternative: Render Blueprint

A Render Blueprint remains available at `backend/render.yaml` for a future deployment attempt. The root-level `render.yaml` is also present for providers that require the Blueprint file at the repository root. If Render requires payment verification, stop rather than attempting to bypass it.

## API safeguards and data reliability

The API uses a process-local sliding-window rate limit (default 120 requests per client IP per minute), a maximum bounding-box degree-area (default 5,000), and bounded concurrent NASA FIRMS requests (default 4). Rate-limited requests receive HTTP 429 and `Retry-After`. Oversized and antimeridian-crossing bounding boxes are rejected.

The limiter uses the direct socket peer address and deliberately does not trust `X-Forwarded-For`, which clients can spoof unless a trusted proxy chain is configured. Behind a reverse proxy, configure trusted-proxy behavior before relying on proxy-derived addresses. The limiter and semaphore are per process; they do not enforce global quotas across multiple instances. Use a shared Redis/API-gateway limiter and shared concurrency controls when horizontally scaling.

CORS is disabled by default for the Android/native-only client. For a browser client, set `FIREPULSE_CORS_ORIGINS` to a comma-separated allowlist of exact origins.

FIRMS responses are checked for CSV structure and required coordinate columns. Invalid/non-finite coordinates are discarded, and non-CSV error pages are rejected. The backend can serve a successful cached response after an upstream error for up to 900 seconds by default (`FIREPULSE_STALE_IF_ERROR_SECONDS`; set to 0 to disable). The API marks these as `data_status: "stale_cache"` and supplies the original `cached_at` timestamp. Do not represent this as live data. Missing `FIRMS_MAP_KEY` is a configuration error and does not use stale fallback.

## Android background refresh

The app registers one unique WorkManager periodic request on launch. It refreshes the default India bounding box (68, 6, 98, 37.5), VIIRS SNPP near-real-time feed, and five-day window every six hours when network connectivity is available. WorkManager execution is inexact and may be deferred by Android battery optimization and system scheduling. Failures use exponential retry backoff. The worker updates the Room snapshot read by Explore and prunes local cache rows older than 30 days. This is best-effort data freshness, not real-time monitoring or alerting.

Explore distinguishes the device's offline Room snapshot, a stale server-side snapshot and a live API response. Stale server responses are not written back into Room as if they were newly retrieved.

## Remaining production security

The prototype still has no user authentication, per-account quotas, distributed/shared rate limiting, persistent/shared cache, comprehensive observability, or formal incident response. Add controls appropriate to the expected audience and hosting topology before public launch. Keep `FIRMS_MAP_KEY` server-side and avoid logging upstream URLs containing the key. Never describe satellite hotspot observations as emergency alerts.

## Android build and verification

Open `android/` in Android Studio and allow Gradle sync. No CI workflow is required. A local build can be performed later with Android Studio or `./gradlew :app:assembleDebug` after a Gradle wrapper is generated. The current branch has not been Gradle-synced, built, or tested on a physical device. Backend tests have been authored but not executed in this environment; source changes remain unverified until tested.
