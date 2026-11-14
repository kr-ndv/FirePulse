from collections import Counter
from datetime import date
from fastapi import APIRouter, HTTPException, Query
from app.services.firms import StaleCacheError, fetch_area_observations
from app.services.clustering import cluster_observations

router = APIRouter(tags=["analytics"])


async def _load_rows(source, west, south, east, north, days, start_date=None):
    try:
        return await fetch_area_observations(source, west, south, east, north, days, start_date), "live", None
    except StaleCacheError as exc:
        return exc.observations, "stale_cache", exc.cached_at
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except RuntimeError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc


@router.get("/summary")
async def summary(
    west: float = Query(..., ge=-180, le=180),
    south: float = Query(..., ge=-90, le=90),
    east: float = Query(..., ge=-180, le=180),
    north: float = Query(..., ge=-90, le=90),
    days: int = Query(1, ge=1, le=5),
    source: str = Query("VIIRS_SNPP_NRT"),
):
    if south >= north or west >= east:
        raise HTTPException(status_code=400, detail="Invalid bounding box")
    rows, data_status, cached_at = await _load_rows(source, west, south, east, north, days)
    by_day = Counter(r["acquisition_date"] for r in rows if r.get("acquisition_date"))
    by_satellite = Counter(r.get("satellite") or "Unknown" for r in rows)
    return {
        "source": source, "days": days, "count": len(rows),
        "retrieved_on": date.today().isoformat(),
        "data_status": data_status, "cached_at": cached_at,
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
    if south >= north or west >= east:
        raise HTTPException(status_code=400, detail="Invalid bounding box")
    rows, data_status, cached_at = await _load_rows(source, west, south, east, north, days)
    features = []
    for row in rows:
        lat, lon = row.get("latitude"), row.get("longitude")
        if lat is None or lon is None:
            continue
        features.append({
            "type": "Feature",
            "geometry": {"type": "Point", "coordinates": [lon, lat]},
            "properties": {k: v for k, v in row.items() if k not in {"latitude", "longitude"}}
        })
    return {
        "type": "FeatureCollection", "features": features, "source": source,
        "count": len(features), "data_status": data_status, "cached_at": cached_at
    }



@router.get("/clusters")
async def fire_clusters(
    west: float = Query(..., ge=-180, le=180),
    south: float = Query(..., ge=-90, le=90),
    east: float = Query(..., ge=-180, le=180),
    north: float = Query(..., ge=-90, le=90),
    days: int = Query(1, ge=1, le=5),
    source: str = Query("VIIRS_SNPP_NRT"),
    radius_km: float = Query(5.0, ge=0.5, le=25.0),
):
    if south >= north or west >= east:
        raise HTTPException(status_code=400, detail="Invalid bounding box")
    rows, data_status, cached_at = await _load_rows(source, west, south, east, north, days)
    try:
        clusters = cluster_observations(rows, radius_km=radius_km)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    return {
        "source": source,
        "days": days,
        "radius_km": radius_km,
        "detection_count": len(rows),
        "cluster_count": len(clusters),
        "data_status": data_status,
        "cached_at": cached_at,
        "scoring_method": "FirePulse heuristic v1: detection count 40%, summed FRP 40%, high-confidence share 20%",
        "clusters": clusters,
    }

 

@router.get("/history")
async def historical_summary(
    west: float = Query(..., ge=-180, le=180),
    south: float = Query(..., ge=-90, le=90),
    east: float = Query(..., ge=-180, le=180),
    north: float = Query(..., ge=-90, le=90),
    start_date: date = Query(..., description="First UTC acquisition date, YYYY-MM-DD"),
    days: int = Query(1, ge=1, le=5),
    source: str = Query("VIIRS_SNPP_SP"),
):
    """Aggregate FIRMS observations for an explicit historical date window."""
    if south >= north or west >= east:
        raise HTTPException(status_code=400, detail="Invalid bounding box")
    if start_date > date.today():
        raise HTTPException(status_code=400, detail="start_date cannot be in the future")
    rows, data_status, cached_at = await _load_rows(
        source, west, south, east, north, days, start_date.isoformat()
    )
    by_day = Counter(r["acquisition_date"] for r in rows if r.get("acquisition_date"))
    by_satellite = Counter(r.get("satellite") or "Unknown" for r in rows)
    return {
        "source": source,
        "days": days,
        "start_date": start_date.isoformat(),
        "end_date": date.fromordinal(start_date.toordinal() + days - 1).isoformat(),
        "count": len(rows),
        "data_status": data_status,
        "cached_at": cached_at,
        "by_day": [{"date": key, "detections": value} for key, value in sorted(by_day.items())],
        "by_satellite": [{"satellite": key, "detections": value} for key, value in sorted(by_satellite.items())],
        "frp_total_mw": round(sum(row.get("frp") or 0 for row in rows), 2),
        "high_confidence_count": sum(
            str(row.get("confidence", "")).lower() in {"h", "high"} for row in rows
        ),
        "note": "Historical satellite detections are observations, not confirmed fire perimeters or burned area.",
    }
