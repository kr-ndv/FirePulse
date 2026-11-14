# FirePulse 🔥

**Satellite fire activity intelligence, powered by NASA observations.**

FirePulse is an Android-first open-source project that organizes NASA MODIS and VIIRS active-fire detections into a consistent spatial and temporal view. It is designed for exploration and research, not emergency response.

> A hotspot is a satellite thermal-anomaly detection. It is not a confirmed fire perimeter, ground-truth incident, or emergency alert.

## Repository
- `android/` — Kotlin, Jetpack Compose and Material 3 Android client.
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

See [API documentation](docs/API.md) and [deployment guide](docs/DEPLOYMENT.md).

## Run Android
Open `android/` in Android Studio, sync Gradle and run the app. Debug emulator traffic targets `http://10.0.2.2:8000/`. A physical Android device needs a reachable LAN URL while developing. Set the HTTPS backend URL in the release BuildConfig before distributing a build.

## Data sources
- [NASA FIRMS](https://firms.modaps.eosdis.nasa.gov/)
- [FIRMS Area API](https://firms.modaps.eosdis.nasa.gov/api/area/)
- [MODIS active fire product](https://modis.gsfc.nasa.gov/data/dataprod/)
- [VIIRS active fire product](https://viirsland.gsfc.nasa.gov/Products/NASA/Fire.html)

## Project status
The repository now includes the Android app shell, initial navigation and theme, Retrofit API contract, FastAPI observations endpoint, GeoJSON feed, summary analytics, deployment blueprint and basic backend tests. A live rendered map, full client-side data flow and persistent offline cache still require integration and device verification. No CI workflow is needed for these changes.

## License
MIT. See [LICENSE](LICENSE).
