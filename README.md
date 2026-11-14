# FirePulse 🔥

**Satellite fire activity intelligence, powered by NASA observations.**

FirePulse is an Android-first open-source project that organizes NASA MODIS and VIIRS active-fire detections into a consistent spatial and temporal view. It is designed for exploration and research, not emergency response.

> A hotspot is a satellite thermal-anomaly detection. It is not a confirmed fire perimeter, ground-truth incident, or emergency alert.

## Repository
- `android/` — Kotlin, Jetpack Compose, Material 3 and MapLibre Native Android client.
- `backend/` — FastAPI service; NASA FIRMS credentials stay server-side.
- `docs/` — architecture, API and deployment notes.

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

See [API documentation](docs/API.md) and [deployment guide](docs/DEPLOYMENT.md).

## Run Android
Open `android/` in Android Studio, sync Gradle and run the app. Debug emulator traffic targets `http://10.0.2.2:8000/`. A physical Android device needs a reachable LAN URL while developing. Set the HTTPS backend URL in the release BuildConfig before distributing a build.

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
The repository includes the Android app shell, navigation and theme, Retrofit data client, MapLibre map layer for FIRMS detections, Room-based offline observation snapshots, periodic WorkManager refresh, FastAPI observations/GeoJSON/summary/clustering endpoints, a short-lived in-memory FIRMS response cache with stale-if-error metadata, FIRMS CSV validation, request rate limiting, bounding-box limits, upstream concurrency controls, deployment blueprint and backend tests. All source changes still require Gradle sync/build, backend test execution and device verification. The API distinguishes live responses from a bounded stale-server fallback (default maximum age 15 minutes); Explore also distinguishes stale server data from its offline Room snapshot. No live FIRMS request or deployment has been performed. Production authentication, distributed quotas, monitoring and deployment remain outstanding. No GitHub Actions workflow is required or used.

## License
MIT. See [LICENSE](LICENSE).


## Geographic exploration

Explore includes broad India geographic-area presets (All India, North, Central, East, West, South and Northeast) that apply rectangular WGS84 search bounds to the FIRMS request and cluster overlay. These are discovery filters, not administrative boundaries: regions overlap and detections near a boundary may be included or excluded according to the rectangle. State- and district-level polygon filtering is not implemented yet. Device offline snapshots currently apply to the All India query only.

## Experimental hotspot clustering
FirePulse can group nearby detections into potential activity clusters and rank them using a documented heuristic based on detection count, summed FRP and high-confidence share. The default neighborhood radius is 5 km. Insights displays the highest-ranked clusters and their coordinates. These groupings are not verified incidents, fire perimeters, official danger ratings or emergency alerts. See [data methodology](docs/DATA_METHODOLOGY.md) for the exact scoring formula and limitations.
