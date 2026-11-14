# Deployment notes

## Current Vercel Preview deployment

The FirePulse FastAPI backend is deployed from `feature/android-foundation` in the Vercel Hobby project. The current branch preview is:

- Base URL: `https://firepulse-gnp3qqg3p-krishnamdev127s-projects.vercel.app/`
- Health: `/health`
- Interactive API docs: `/docs`
- Runtime region: Mumbai (`bom1`)

The health endpoint and Swagger documentation have been opened successfully in a browser. This confirms that the FastAPI application is deployed and its routes are registered. It does **not** yet prove that a live NASA FIRMS query succeeds; that must be tested through a bounded `/api/v1/fires` request.

The Android release BuildConfig now targets this Preview URL. Debug emulator builds continue to use `http://10.0.2.2:8000/`. The preview URL is suitable for integration testing only; configure a stable production domain and update Android `API_BASE_URL` before public distribution.

## Deploy the FastAPI backend to Vercel

FirePulse includes a Vercel-compatible FastAPI entrypoint at `backend/index.py`. Vercel's Python runtime detects the exported FastAPI `app` and deploys the backend as a Function. The project is pinned to Python 3.12 through `backend/.python-version`.

1. Import the GitHub repository `kr-ndv/FirePulse` into Vercel.
2. Select the `feature/android-foundation` branch while testing this implementation. Do not change or merge `main`.
3. Set **Root Directory** to `backend`. The FastAPI entrypoint and `requirements.txt` are inside that directory.
4. Leave the Framework Preset as **Other** or accept Vercel's FastAPI/Python detection. Do not add a custom Build Command or Output Directory.
5. Add `FIRMS_MAP_KEY` as an encrypted environment variable for Preview. Add it to Production only when ready. Do not put the key in GitHub, Android source, or a client-side variable.
6. Deploy the project. Vercel's deployment build is separate from GitHub Actions; this project does not require an Actions workflow.

After deployment, verify:
- `/health` returns a JSON status response.
- `/docs` opens the FastAPI Swagger UI.
- A bounded request to `/api/v1/fires` returns observations when `FIRMS_MAP_KEY` is valid and NASA FIRMS is available.

### Vercel runtime considerations

Vercel runs this FastAPI application as a serverless Function, not as a continuously running Uvicorn server. Function instances can start, stop, and scale independently. FirePulse's in-memory rate limiter, semaphore, and response cache are therefore per-instance and may reset or differ between instances; they are not global quotas or durable storage. The Android Room cache remains the app's offline cache.

The NASA FIRMS request timeout and Vercel Function duration must be compatible. If a request times out in deployment, inspect Vercel Function logs and reduce the requested workload or adjust supported function duration within the selected plan's limits. Do not claim that Vercel deployment is verified until the live endpoints have been checked.

Vercel's Hobby plan is intended for personal, non-commercial projects. Plan eligibility, usage limits, signup verification and payment checks are controlled by Vercel. This repository configuration cannot guarantee a no-card signup.

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
