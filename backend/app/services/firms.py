import csv
import io
import os
import httpx

ALLOWED_SOURCES = {"MODIS_NRT", "VIIRS_SNPP_NRT", "VIIRS_NOAA20_NRT", "VIIRS_NOAA21_NRT"}

async def fetch_area_observations(source: str, west: float, south: float,
                                  east: float, north: float, days: int) -> list[dict]:
    if source not in ALLOWED_SOURCES:
        raise ValueError(f"Unsupported FIRMS source: {source}")
    map_key = os.getenv("FIRMS_MAP_KEY")
    if not map_key:
        raise RuntimeError("FIRMS_MAP_KEY is not configured on the server")
    base = os.getenv("FIRMS_BASE_URL", "https://firms.modaps.eosdis.nasa.gov/api/area/csv")
    area = f"{west},{south},{east},{north}"
    url = f"{base}/{map_key}/{source}/{area}/{days}"
    async with httpx.AsyncClient(timeout=30) as client:
        response = await client.get(url)
        response.raise_for_status()
    rows = csv.DictReader(io.StringIO(response.text))
    return [{
        "latitude": _num(row.get("latitude")),
        "longitude": _num(row.get("longitude")),
        "acquisition_date": row.get("acq_date"),
        "acquisition_time": row.get("acq_time"),
        "satellite": row.get("satellite"),
        "instrument": row.get("instrument"),
        "confidence": row.get("confidence"),
        "frp": _num(row.get("frp")),
    } for row in rows]

def _num(value):
    try:
        return float(value) if value not in (None, "", "null") else None
    except (TypeError, ValueError):
        return None
