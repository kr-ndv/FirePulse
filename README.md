# FirePulse 🔥

**Satellite fire activity intelligence, powered by NASA observations.**

FirePulse is an Android-first open-source project that organizes NASA MODIS and VIIRS active-fire detections into a consistent spatial and temporal view. It is designed for exploration and research, not emergency response.

> A hotspot is a satellite thermal-anomaly detection. It is not a confirmed fire perimeter, ground-truth incident, or emergency alert.

## Repository
- `android/` — Kotlin, Jetpack Compose, Material 3 and MapLibre Native Android client.
- `backend/` — FastAPI service; NASA FIRMS credentials stay server-side.
- `docs/` — architecture, API and deployment notes.

## Live API preview
The current testing deployment is available at:
- Health: https://firepulse-gnp3qqg3p-krishnamdev127s-projects.vercel.app/health
- Interactive API docs: https://firepulse-gnp3qqg3p-krishnamdev127s-projects.vercel.app/docs

This is a Vercel Preview deployment from `feature/android-foundation`, not a production release. The Android release build is configured to use this preview URL for end-to-end testing. Replace it with a stable production domain before publishing the app.

## NASA API credential
The backend requires a free NASA FIRMS MAP_KEY. Request it from [NASA FIRMS Area API](https://firms.modaps.eosdis.nasa.gov/api/area/). Store it only as the backend environment variable `FIRMS_MAP_KEY`. **There is no API key to put in the Android app.** Never commit the real key.

## Run the backend
```bash
cd backend
python -m venv .venv
# Windows: .venv\\Scripts\\activate
# macOS/Linux: source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
# Add FIRMS_MAP_KEY to .env
uvicorn app.main:app --reload
```
- Health: `http://localhost:8000/health`
- API docs: `http://localhost:8000/docs`
- Fire observations: `/api/v1/fires`
- GeoJSON map feed: `/api/v1/fires/geojson`
- Analytics summary: `/api/v1/summary`
- Potential fire-activity clusters: `/api/v1/clusters` (experimental heuristic, not an official severity rating)

See [API documentation](docs/API.md) and [deployment guide](docs/DEPLOYMENT.md). The FastAPI backend includes a Vercel entrypoint; when importing the repository into Vercel, set the project Root Directory to `backend` and configure `FIRMS_MAP_KEY` as an environment variable.

## Run Android
Open `android/` in Android Studio, sync Gradle and run the app. Debug emulator traffic targets `http://10.0.2.2:8000/`. A physical Android device needs a reachable LAN URL while developing. The release build currently targets the FirePulse Vercel Preview deployment; update `API_BASE_URL` in `android/app/build.gradle.kts` when moving to a stable production API domain.

## Background refresh and offline snapshots
The Android app schedules a unique WorkManager job to refresh the default India-area VIIRS SNPP five-day snapshot approximately every six hours when network connectivity is available. Android may defer periodic work due to battery and system scheduling policies; this is not a real-time feed. Successful refreshes replace the matching Room snapshot, which remains available to the Explore screen offline. Failed refreshes are retried with backoff. The worker does not issue notifications or emergency alerts.

## Backend request safeguards
The API has a process-local sliding-window rate limit (default 120 requests per client IP per minute), bounding-box size limit (default 5,000 degree-squared), and upstream concurrency cap (default 4 concurrent NASA FIRMS calls). These are configurable through environment variables documented in `backend/.env.example`. CORS is disabled by default for the native Android client. These in-process controls are a prototype baseline, not distributed production quotas; use a shared gateway/Redis limiter before scaling to multiple instances.

## Data sources
- [NASA FIRMS](https://firms.modaps.eosdis.nasa.gov/)
- [FIRMS Area API](https://firms.modaps.eosdis.nasa.gov/api/area/)
- [MODIS active fire product](https://modis.gsfc.nasa.gov/data/dataprod/)
- [VIIRS active fire product](https://viirsland.gsfc.nasa.gov/Products/NASA/Fire.html)

## Project status
The repository includes the Android app shell, navigation and theme, Retrofit data client, MapLibre map layer for FIRMS detections, Room-based offline observation snapshots, periodic WorkManager refresh, FastAPI observations/GeoJSON/summary/clustering/historical endpoints, a short-lived in-memory FIRMS response cache with stale-if-error metadata, FIRMS CSV validation, request rate limiting, bounding-box limits, upstream concurrency controls and deployment configuration. The Vercel Preview deployment has passed live health and API documentation checks. Android Gradle sync/build, backend test execution, live FIRMS data-path verification and physical-device verification remain outstanding. No GitHub Actions workflow is required or used.

## License
MIT. See [LICENSE](LICENSE).

## Geographic exploration
Explore includes broad India geographic-area presets (All India, North, Central, East, West, South and Northeast) that apply rectangular WGS84 search bounds to the FIRMS request and cluster overlay. These are discovery filters, not administrative boundaries: regions overlap and detections near a boundary may be included or excluded according to the rectangle. State- and district-level polygon filtering is not implemented yet. Device offline snapshots currently apply to the All India query only.

## Experimental hotspot clustering
FirePulse can group nearby detections into potential activity clusters and rank them using a documented heuristic based on detection count, summed FRP and high-confidence share. The default neighborhood radius is 5 km. Insights displays the highest-ranked clusters and their coordinates. These groupings are not verified incidents, fire perimeters, official danger ratings or emergency alerts. See [data methodology](docs/DATA_METHODOLOGY.md) for the exact scoring formula and limitations.

## Historical activity
Insights includes a date-scoped archive query using NASA FIRMS Standard Processing (SP) products. Select a start date to retrieve a five-day historical summary for India, including detection counts, high-confidence detections and summed FRP. SP products can have a processing lag and should not be treated as directly interchangeable with near-real-time NRT feeds. The API limits each request to five days; it does not fabricate missing dates or infer historical fire spread.
