import csv
import io
import os
import httpx

ALLOWED_SOURCES = {"MODIS_NRT", "VIIRS_SNPP_NRT", "VIIRS_NOAA20_NRT", "VIIRS_NOAA21_NRT"}

async def fetch_area_observations(source: str, west: float, south: float,
                                  east: float, north: float, days: int) -> list[dict]:
    if source not in ALLOWED_SOURCES:
        raise ValueError(f"Unsupported FIRMS source: {source}")
    if not (-180 <= west <= 180 and -180 <= east <= 180 and -90 <= south < north <= 90):
        raise ValueError("Invalid WGS84 bounding box")
    if west == east:
        raise ValueError("west and east must differ")
    if not 1 <= days <= 5:
        raise ValueError("days must be between 1 and 5")
    map_key = os.getenv("FIRMS_MAP_KEY")
    if not map_key:
        raise RuntimeError("FIRMS_MAP_KEY is not configured on the server")
    base = os.getenv("FIRMS_BASE_URL", "https://firms.modaps.eosdis.nasa.gov/api/area/csv").rstrip("/")
    area = f"{west},{south},{east},{north}"
    url = f"{base}/{map_key}/{source}/{area}/{days}"
    async with httpx.AsyncClient(timeout=httpx.Timeout(25.0, connect=8.0),
                                 headers={"User-Agent": "FirePulse/0.2 (+https://github.com/kr-ndv/FirePulse)"}) as client:
        response = await client.get(url)
        response.raise_for_status()
    reader = csv.DictReader(io.StringIO(response.text))
    if not reader.fieldnames or "latitude" not in reader.fieldnames or "longitude" not in reader.fieldnames:
        raise RuntimeError("NASA FIRMS returned an unexpected response; verify the MAP_KEY, source and request limits")
    observations = []
    for row in reader:
        latitude, longitude = _num(row.get("latitude")), _num(row.get("longitude"))
        if latitude is None or longitude is None:
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
    return observations

def _num(value):
    try:
        return float(value) if value not in (None, "", "null") else None
    except (TypeError, ValueError):
        return None
