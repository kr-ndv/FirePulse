# FirePulse API

Base URL is the deployed FastAPI service origin. Interactive OpenAPI documentation is exposed at `/docs` and the schema at `/openapi.json`.

## Authentication and secrets
The public prototype API does not implement user authentication. Do not expose a production deployment without adding authentication, quotas and abuse protection. The NASA FIRMS `MAP_KEY` is a server-side environment secret named `FIRMS_MAP_KEY`; never place it in Android resources, BuildConfig, source control or client requests.

## Endpoints
- `GET /health` — liveness check.
- `GET /api/v1/fires?west=...&south=...&east=...&north=...&days=1&source=VIIRS_SNPP_NRT` — normalized active-fire observations.
- `GET /api/v1/fires/geojson?...same filters...` — GeoJSON FeatureCollection for map clients.
- `GET /api/v1/summary?...same filters...` — total detections, per-day and per-satellite counts, summed FRP and high-confidence count.
- `GET /api/v1/clusters?...same filters...&radius_km=5` — proximity-grouped hotspot clusters and an explicitly heuristic activity score. `radius_km` is limited to 0.5–25 km.

## Cache and freshness
Successful FIRMS responses are cached in process memory for 60 seconds (maximum 128 request keys). If NASA FIRMS fails or returns a malformed/non-CSV response, the API may serve the last successful response for the same request if it is no older than `FIREPULSE_STALE_IF_ERROR_SECONDS` (default 900 seconds). Such responses carry `data_status: "stale_cache"` and an ISO-8601 UTC `cached_at` timestamp. Fresh upstream responses carry `data_status: "live"` and `cached_at: null`. Set the stale window to 0 to disable this fallback. Missing credentials do not trigger stale fallback.

The cache is process-local, non-persistent and not shared between server instances. A stale response is not a current observation; clients must label it clearly and must not reset their local freshness clock when storing it.

## Request constraints
Bounding coordinates are WGS84 degrees. Days are limited to 1–5. Supported sources are MODIS_NRT, VIIRS_SNPP_NRT, VIIRS_NOAA20_NRT and VIIRS_NOAA21_NRT. The east/west dateline-crossing case is not supported. CSV headers are normalized for whitespace/case, invalid coordinates and non-finite numeric values are discarded, and HTML/JSON error pages are rejected rather than presented as an empty fire dataset.

## Example
```bash
curl "http://localhost:8000/api/v1/fires?west=75&south=8&east=90&north=22&days=1&source=VIIRS_SNPP_NRT"
```

A detection is a satellite thermal anomaly, not a confirmed fire perimeter or proof of ground conditions. Acquisition time is supplied by FIRMS and should be treated as UTC unless the source documentation specifies otherwise.


## Fire-activity clusters
The clusters endpoint groups observations whose great-circle separation is within the requested radius. It uses connected-component proximity grouping: a chain of nearby detections can form one cluster even if the two most distant detections in that cluster are farther apart than the radius. The response includes centroid, detection count, summed FRP, high-confidence count, acquisition-date span, satellite list and a score.

FirePulse heuristic v1 computes a 0–100 score from three capped components: detection count contributes up to 40 points (10 detections saturates this component), summed FRP contributes up to 40 points (100 MW saturates this component), and the share of high-confidence detections contributes up to 20 points. Labels are low (<25), moderate (25–49.9), elevated (50–74.9) and high (75–100). This is an experimental ranking heuristic, not a validated fire-danger index, incident severity assessment, probability of spread, or official warning. It does not account for wind, fuel, terrain, weather, population exposure, active fire perimeter, or sensor sampling bias.

The endpoint limits input to 5,000 observations and radius to 0.5–25 km. It returns the same freshness metadata as other FIRMS-backed endpoints. Do not interpret clusters as confirmed incidents.


## Historical activity summary
- `GET /api/v1/history?west=...&south=...&east=...&north=...&start_date=YYYY-MM-DD&days=1..5&source=VIIRS_SNPP_SP`
- Requests an explicit date window. NASA FIRMS interprets the date as the first day and returns that day through `start_date + days - 1`.
- Supports the matching Standard Processing archive products: `MODIS_SP`, `VIIRS_SNPP_SP`, `VIIRS_NOAA20_SP`, and `VIIRS_NOAA21_SP`. NRT source IDs are also accepted for date-scoped requests where NASA still has those dates available.
- The endpoint returns total detections, per-day and per-satellite counts, summed FRP, high-confidence count, requested date bounds and cache freshness metadata.
- Dates must be valid ISO dates and cannot start in the future. Each request is limited to five days, matching the FIRMS Area API contract. For longer periods, clients should request separate windows rather than silently issuing a large burst of upstream transactions.
- Standard Processing (SP) products are science-quality archive products and may have a processing lag. Historical results should not be compared directly with NRT counts without noting product and processing differences.
- Historical detections remain thermal anomalies, not confirmed fire perimeters, burned area or incident records.
