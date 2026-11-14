from collections import Counter
from datetime import date
from fastapi import APIRouter, HTTPException, Query
from app.services.firms import fetch_area_observations

router = APIRouter(tags=["analytics"])

@router.get("/summary")
async def summary(
    west: float = Query(..., ge=-180, le=180),
    south: float = Query(..., ge=-90, le=90),
    east: float = Query(..., ge=-180, le=180),
    north: float = Query(..., ge=-90, le=90),
    days: int = Query(1, ge=1, le=5),
    source: str = Query("VIIRS_SNPP_NRT"),
):
    if south >= north or west == east:
        raise HTTPException(status_code=400, detail="Invalid bounding box")
    try:
        rows = await fetch_area_observations(source, west, south, east, north, days)
    except (ValueError, RuntimeError) as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc
    by_day = Counter(r["acquisition_date"] for r in rows if r.get("acquisition_date"))
    by_satellite = Counter(r.get("satellite") or "Unknown" for r in rows)
    return {
        "source": source, "days": days, "count": len(rows),
        "retrieved_on": date.today().isoformat(),
        "by_day": [{"date": k, "detections": v} for k, v in sorted(by_day.items())],
        "by_satellite": [{"satellite": k, "detections": v} for k, v in sorted(by_satellite.items())],
        "frp_total_mw": round(sum(r.get("frp") or 0 for r in rows), 2),
        "high_confidence_count": sum(str(r.get("confidence", "")).lower() in {"h", "high"} for r in rows)
    }

@router.get("/fires/geojson")
async def fires_geojson(
    west: float = Query(..., ge=-180, le=180),
    south: float = Query(..., ge=-90, le=90),
    east: float = Query(..., ge=-180, le=180),
    north: float = Query(..., ge=-90, le=90),
    days: int = Query(1, ge=1, le=5),
    source: str = Query("VIIRS_SNPP_NRT"),
):
    if south >= north or west == east:
        raise HTTPException(status_code=400, detail="Invalid bounding box")
    try:
        rows = await fetch_area_observations(source, west, south, east, north, days)
    except (ValueError, RuntimeError) as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc
    features = []
    for row in rows:
        lat, lon = row.get("latitude"), row.get("longitude")
        if lat is None or lon is None:
            continue
        features.append({"type": "Feature", "geometry": {"type": "Point", "coordinates": [lon, lat]},
                         "properties": {k: v for k, v in row.items() if k not in {"latitude", "longitude"}}})
    return {"type": "FeatureCollection", "features": features, "source": source, "count": len(features)}
