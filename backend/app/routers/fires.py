from datetime import date
from fastapi import APIRouter, HTTPException, Query
from app.services.firms import StaleCacheError, fetch_area_observations

router = APIRouter(tags=["fire observations"])


@router.get("/fires")
async def get_fires(
    west: float = Query(..., ge=-180, le=180),
    south: float = Query(..., ge=-90, le=90),
    east: float = Query(..., ge=-180, le=180),
    north: float = Query(..., ge=-90, le=90),
    days: int = Query(1, ge=1, le=5),
    source: str = Query("VIIRS_SNPP_NRT"),
):
    if south >= north or west >= east:
        raise HTTPException(status_code=400, detail="Invalid bounding box; south must be below north and west below east")
    try:
        observations = await fetch_area_observations(source, west, south, east, north, days)
        data_status, cached_at = "live", None
    except StaleCacheError as exc:
        observations, data_status, cached_at = exc.observations, "stale_cache", exc.cached_at
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except RuntimeError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc
    return {
        "source": source,
        "count": len(observations),
        "retrieved_on": date.today().isoformat(),
        "data_status": data_status,
        "cached_at": cached_at,
        "observations": observations,
    }
