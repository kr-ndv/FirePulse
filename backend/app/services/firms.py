import csv
import io
import os
import time
import asyncio

import httpx

ALLOWED_SOURCES = {"MODIS_NRT", "VIIRS_SNPP_NRT", "VIIRS_NOAA20_NRT", "VIIRS_NOAA21_NRT"}
_CACHE_TTL_SECONDS = 60
_CACHE_MAX_ENTRIES = 128
_MAX_BBOX_DEGREE_AREA = float(os.getenv("FIREPULSE_MAX_BBOX_DEGREE_AREA", "5000"))
_MAX_UPSTREAM_CONCURRENCY = max(1, int(os.getenv("FIREPULSE_MAX_UPSTREAM_CONCURRENCY", "4")))
_cache: dict[tuple, tuple[float, list[dict]]] = {}
_cache_lock = asyncio.Lock()
_upstream_semaphore = asyncio.Semaphore(_MAX_UPSTREAM_CONCURRENCY)


async def fetch_area_observations(source: str, west: float, south: float,
                                  east: float, north: float, days: int) -> list[dict]:
    if source not in ALLOWED_SOURCES:
        raise ValueError(f"Unsupported FIRMS source: {source}")
    if not (-180 <= west <= 180 and -180 <= east <= 180 and -90 <= south < north <= 90):
        raise ValueError("Invalid WGS84 bounding box")
    if west >= east:
        raise ValueError("west must be lower than east; antimeridian-crossing boxes are not supported")
    if (east - west) * (north - south) > _MAX_BBOX_DEGREE_AREA:
        raise ValueError(
            f"Bounding box is too large; maximum degree-area is {_MAX_BBOX_DEGREE_AREA:g}"
        )
    if not 1 <= days <= 5:
        raise ValueError("days must be between 1 and 5")

    cache_key = (source, west, south, east, north, days)
    now = time.monotonic()
    async with _cache_lock:
        cached = _cache.get(cache_key)
        if cached and now - cached[0] < _CACHE_TTL_SECONDS:
            return [dict(item) for item in cached[1]]
        if cached:
            _cache.pop(cache_key, None)

    map_key = os.getenv("FIRMS_MAP_KEY")
    if not map_key:
        raise RuntimeError("FIRMS_MAP_KEY is not configured on the server")
    base = os.getenv(
        "FIRMS_BASE_URL",
        "https://firms.modaps.eosdis.nasa.gov/api/area/csv",
    ).rstrip("/")
    area = f"{west},{south},{east},{north}"
    url = f"{base}/{map_key}/{source}/{area}/{days}"

    try:
        # Bound concurrent upstream calls so a burst of distinct requests
        # cannot exhaust backend sockets or overwhelm NASA FIRMS.
        async with _upstream_semaphore:
            async with httpx.AsyncClient(
                timeout=httpx.Timeout(25.0, connect=8.0),
                headers={"User-Agent": "FirePulse/0.3"},
            ) as client:
                response = await client.get(url)
                response.raise_for_status()
    except httpx.HTTPError as exc:
        raise RuntimeError("NASA FIRMS request failed or timed out") from exc

    reader = csv.DictReader(io.StringIO(response.text))
    if not reader.fieldnames or "latitude" not in reader.fieldnames or "longitude" not in reader.fieldnames:
        raise RuntimeError(
            "NASA FIRMS returned an unexpected response; verify the MAP_KEY, source and request limits"
        )

    observations = []
    for row in reader:
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

    async with _cache_lock:
        if len(_cache) >= _CACHE_MAX_ENTRIES:
            oldest_key = min(_cache, key=lambda key: _cache[key][0])
            _cache.pop(oldest_key, None)
        _cache[cache_key] = (time.monotonic(), observations)
    return [dict(item) for item in observations]


def _num(value):
    try:
        return float(value) if value not in (None, "", "null") else None
    except (TypeError, ValueError):
        return None
