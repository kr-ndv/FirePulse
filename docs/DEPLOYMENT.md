# Deployment notes

## API hosting
The backend is a stateless FastAPI service. A Render Blueprint is provided in `backend/render.yaml`. Create the service from the repository, then set `FIRMS_MAP_KEY` in the hosting dashboard. The key is obtained from NASA FIRMS: https://firms.modaps.eosdis.nasa.gov/api/area/ . Do not commit the real key.

After deployment, verify `/health`, `/docs`, and one bounded `/api/v1/fires` request. Then replace the Android release API URL with the HTTPS service origin (include trailing slash). Keep the debug emulator URL separate.

## Security before public launch
This prototype has no user authentication, request quotas, persistent cache, or abuse controls. Add a rate limiter, bounded request concurrency, response caching, monitoring, and an authentication strategy before exposing it to untrusted public traffic. Restrict CORS to actual web origins if a web client is added. Do not describe satellite hotspot observations as emergency alerts.

## Android build
Open the `android/` directory in Android Studio and allow Gradle sync. No CI workflow is required. A local build can be performed later with Android Studio or `./gradlew :app:assembleDebug` after a Gradle wrapper is generated.
