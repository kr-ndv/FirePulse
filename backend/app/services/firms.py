import csv
import io
import os
import time
import asyncio
from datetime import datetime, timezone

import httpx

ALLOWED_SOURCES = {"MODIS_NRT", "VIIRS_SNPP_NRT", "VIIRS_NOAA20_NRT", "VIIRS_NOAA21_NRT"}
_CACHE_TTL_SECONDS = 60
_CACHE_MAX_ENTRIES = 128
_STALE_IF_ERROR_SECONDS = max(0, int(os.getenv("FIREPULSE_STALE_IF_ERROR_SECONDS", "900")))
_MAX_BBOX_DEGREE_AREA = float(os.getenv("FIREPULSE_MAX_BBOX_DEGREE_AREA", "5000"))
_MAX_UPSTREAM_CONCURRENCY = max(1, int(os.getenv("FIREPULSE_MAX_UPSTREAM_CONCURRENCY", "4")))
# Cache tuple: (monotonic timestamp, UTC timestamp, observations).
_cache: dict[tuple, tuple[float, str, list[dict]]] = {}
_cache_lock = asyncio.Lock()
_upstream_semaphore = asyncio.Semaphore(_MAX_UPSTREAM_CONCURRENCY)


class StaleCacheError(RuntimeError):
    """Raised when stale-but-recent cached data is served after an upstream failure."""

    def __init__(self, observations: list[dict], cached_at: str):
        super().__init__("NASA FIRMS is temporarily unavailable; returning a recent cached snapshot")
        self.observations = [dict(item) for item in observations]
        self.cached_at = cached_at


def _num(value):
    try:
        number = float(value) if value not in (None, "", "null") else None
        return number if number is None or (number == number and abs(number) != float("inf")) else None
    except (TypeError, ValueError):
        return None


async def fetch_area_observations(source: str, west: float, south: float,
                                  east: float, north: float, days: int) -> list[dict]:
    if source not in ALLOWED_SOURCES:
        raise ValueError(f"Unsupported FIRMS source: {source}")
    if not (-180 <= west <= 180 and -180 <= east <= 180 and -90 <= south < north <= 90):
        raise ValueError("Invalid WGS84 bounding box")
    if west >= east:
        raise ValueError("west must be lower than east; antimeridian-crossing boxes are not supported")
    if (east - west) * (north - south) > _MAX_BBOX_DEGREE_AREA:
        raise ValueError(f"Bounding box is too large; maximum degree-area is {_MAX_BBOX_DEGREE_AREA:g}")
    if not 1 <= days <= 5:
        raise ValueError("days must be between 1 and 5")

    cache_key = (source, west, south, east, north, days)
    now = time.monotonic()
    cached = None
    async with _cache_lock:
        cached = _cache.get(cache_key)
        if cached and now - cached[0] < _CACHE_TTL_SECONDS:
            return [dict(item) for item in cached[2]]
        if cached and now - cached[0] > _STALE_IF_ERROR_SECONDS:
            _cache.pop(cache_key, None)
            cached = None

    map_key = os.getenv("FIRMS_MAP_KEY")
    if not map_key:
        raise RuntimeError("FIRMS_MAP_KEY is not configured on the server")
    base = os.getenv("FIRMS_BASE_URL", "https://firms.modaps.eosdis.nasa.gov/api/area/csv").rstrip("/")
    area = f"{west},{south},{east},{north}"
    url = f"{base}/{map_key}/{source}/{area}/{days}"

    try:
        async with _upstream_semaphore:
            async with httpx.AsyncClient(
                timeout=httpx.Timeout(25.0, connect=8.0),
                headers={"User-Agent": "FirePulse/0.3"},
            ) as client:
                response = await client.get(url)
                response.raise_for_status()
        body = response.text
        # FIRMS should return CSV. HTML/proxy error pages and JSON error payloads
        # must never be interpreted as a successful empty fire dataset.
        if not body.strip() or body.lstrip().startswith(("<", "{", "[")):
            raise RuntimeError("NASA FIRMS returned an empty or non-CSV response")
        reader = csv.DictReader(io.StringIO(body))
        headers = {name.strip().lower() for name in (reader.fieldnames or []) if name}
        if not {"latitude", "longitude"}.issubset(headers):
            raise RuntimeError("NASA FIRMS returned an unexpected CSV schema")
        # Normalize header whitespace/case before reading fields.
        rows = [{(key or "").strip().lower(): value for key, value in row.items()} for row in reader]
        observations = []
        for row in rows:
            latitude, longitude = _num(row.get("latitude")), _num(row.get("longitude"))
            if latitude is None or longitude is None:
                continue
            if not (-90 <= latitude <= 90 and -180 <= longitude <= 180):
                continue
            observations.append({
                "latitude": latitude,
                "longitude": longitude,
                "acquisition_date": row.get("acq_date"),
                "acquisition_time": row.get("acq_time"),
                "satellite": row.get("satellite"),
                "instrument": row.get("instrument"),
                "confidence": row.get("confidence"),
                "frp": _num(row.get("frp")),
                "brightness": _num(row.get("bright_ti4") or row.get("brightness")),
                "daynight": row.get("daynight"),
            })
    except (httpx.HTTPError, RuntimeError) as exc:
        if cached and _STALE_IF_ERROR_SECONDS > 0:
            raise StaleCacheError(cached[2], cached[1]) from exc
        if isinstance(exc, RuntimeError):
            raise
        raise RuntimeError("NASA FIRMS request failed or timed out") from exc

    retrieved_at = datetime.now(timezone.utc).isoformat()
    async with _cache_lock:
        if len(_cache) >= _CACHE_MAX_ENTRIES and cache_key not in _cache:
            oldest_key = min(_cache, key=lambda key: _cache[key][0])
            _cache.pop(oldest_key, None)
        _cache[cache_key] = (time.monotonic(), retrieved_at, observations)
    return [dict(item) for item in observations]
