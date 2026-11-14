# FirePulse API

Base URL is the deployed FastAPI service origin. Interactive OpenAPI documentation is exposed at `/docs` and the schema at `/openapi.json`.

## Authentication and secrets
The public prototype API does not implement user authentication. Do not expose a production deployment without adding authentication, quotas and abuse protection. The NASA FIRMS `MAP_KEY` is a server-side environment secret named `FIRMS_MAP_KEY`; never place it in Android resources, BuildConfig, source control or client requests.

## Endpoints
- `GET /health` — liveness check.
- `GET /api/v1/fires?west=...&south=...&east=...&north=...&days=1&source=VIIRS_SNPP_NRT` — normalized active-fire observations.
- `GET /api/v1/fires/geojson?...same filters...` — GeoJSON FeatureCollection for map clients.
- `GET /api/v1/summary?...same filters...` — total detections, per-day and per-satellite counts, summed FRP and high-confidence count.

Bounding coordinates are WGS84 degrees. Days are limited to 1–5. Supported sources are MODIS_NRT, VIIRS_SNPP_NRT, VIIRS_NOAA20_NRT and VIIRS_NOAA21_NRT. The east/west dateline-crossing case is not supported by the current FIRMS Area API wrapper.

## Example
```bash
curl "http://localhost:8000/api/v1/fires?west=75&south=8&east=90&north=22&days=1&source=VIIRS_SNPP_NRT"
```

A detection is a satellite thermal anomaly, not a confirmed fire perimeter or proof of ground conditions. Acquisition time is supplied by FIRMS and should be treated as UTC unless the source documentation specifies otherwise.
