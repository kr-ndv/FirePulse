# FirePulse API

Base URL is the deployed FastAPI service origin. Interactive OpenAPI documentation is exposed at `/docs` and the schema at `/openapi.json`.

## Authentication and secrets
The public prototype API does not implement user authentication. Do not expose a production deployment without adding authentication, quotas and abuse protection. The NASA FIRMS `MAP_KEY` is a server-side environment secret named `FIRMS_MAP_KEY`; never place it in Android resources, BuildConfig, source control or client requests.

## Endpoints
- `GET /health` — liveness check.
- `GET /api/v1/fires?west=...&south=...&east=...&north=...&days=1&source=VIIRS_SNPP_NRT` — normalized active-fire observations.
- `GET /api/v1/fires/geojson?...same filters...` — GeoJSON FeatureCollection for map clients.
- `GET /api/v1/summary?...same filters...` — total detections, per-day and per-satellite counts, summed FRP and high-confidence count.

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
