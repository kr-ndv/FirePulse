# FirePulse 🔥

**Satellite fire activity intelligence, powered by NASA observations.**

FirePulse is an Android-first open-source project exploring how NASA MODIS and VIIRS active-fire observations can be organized into a consistent spatial and temporal view.

> A hotspot is a satellite thermal-anomaly detection, not a confirmed fire perimeter. FirePulse is for research and situational awareness, not emergency alerts.

## Repository

- `android/` — Kotlin, Jetpack Compose and Material 3 Android application.
- `backend/` — FastAPI service that securely accesses NASA FIRMS.
- `docs/` — Architecture and data methodology.

## Run the backend

```bash
cd backend
python -m venv .venv
# Windows: .venv\Scripts\activate
# macOS/Linux: source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
# Set FIRMS_MAP_KEY in .env
uvicorn app.main:app --reload
```

Health endpoint: `http://localhost:8000/health`.

## Run Android

Open `android/` in Android Studio, sync Gradle and run the `app` configuration. The debug emulator endpoint uses `http://10.0.2.2:8000/`. For a physical device, use your development machine's LAN IP. Set a production HTTPS endpoint before release.

## NASA resources

- [NASA FIRMS](https://firms.modaps.eosdis.nasa.gov/)
- [FIRMS Area API](https://firms.modaps.eosdis.nasa.gov/api/area/)
- [Request a FIRMS MAP_KEY](https://firms.modaps.eosdis.nasa.gov/api/area/)

Keep the MAP_KEY on the backend only. Never commit it or embed it in the Android client.

## Status

Initial foundation: single-module Android app shell, custom Material 3 theme, navigation, Retrofit contract and FastAPI FIRMS integration skeleton. Interactive map, live data presentation, Room caching, complete sensor harmonization, tests and deployment are next.

## License

MIT. See [LICENSE](LICENSE).
