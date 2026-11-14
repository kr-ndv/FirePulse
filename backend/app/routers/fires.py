from datetime import date
from fastapi import APIRouter, HTTPException, Query
from app.services.firms import fetch_area_observations

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
    if south >= north:
        raise HTTPException(status_code=400, detail="south must be lower than north")
    if west == east:
        raise HTTPException(status_code=400, detail="west and east must differ")
    try:
        observations = await fetch_area_observations(source, west, south, east, north, days)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except RuntimeError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc
    return {"source": source, "count": len(observations),
            "retrieved_on": date.today().isoformat(), "observations": observations}
