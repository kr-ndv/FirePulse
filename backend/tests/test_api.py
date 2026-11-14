from fastapi.testclient import TestClient
from app.main import app
from app.routers import fires, analytics
from app.services.firms import StaleCacheError

client = TestClient(app)

SAMPLE = [{
    "latitude": 25.1,
    "longitude": 78.2,
    "acquisition_date": "2026-11-14",
    "acquisition_time": "0512",
    "satellite": "N",
    "instrument": "VIIRS",
    "confidence": "h",
    "frp": 12.4,
    "brightness": 320.1,
    "daynight": "D",
}]


async def fake_observations(*args, **kwargs):
    return SAMPLE


def test_health_endpoint():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "ok"


def test_fires_endpoint_returns_normalized_observations(monkeypatch):
    monkeypatch.setattr(fires, "fetch_area_observations", fake_observations)
    response = client.get("/api/v1/fires", params={
        "west": 68, "south": 6, "east": 98, "north": 37.5
    })
    assert response.status_code == 200
    assert response.json()["count"] == 1
    assert response.json()["observations"][0]["latitude"] == 25.1


def test_geojson_endpoint_returns_longitude_latitude_order(monkeypatch):
    monkeypatch.setattr(analytics, "fetch_area_observations", fake_observations)
    response = client.get("/api/v1/fires/geojson", params={
        "west": 68, "south": 6, "east": 98, "north": 37.5
    })
    assert response.status_code == 200
    feature = response.json()["features"][0]
    assert feature["geometry"]["coordinates"] == [78.2, 25.1]


def test_summary_endpoint_aggregates_observations(monkeypatch):
    monkeypatch.setattr(analytics, "fetch_area_observations", fake_observations)
    response = client.get("/api/v1/summary", params={
        "west": 68, "south": 6, "east": 98, "north": 37.5
    })
    assert response.status_code == 200
    body = response.json()
    assert body["count"] == 1
    assert body["frp_total_mw"] == 12.4
    assert body["high_confidence_count"] == 1


def test_fires_endpoint_rejects_invalid_source():
    response = client.get("/api/v1/fires", params={
        "west": 68, "south": 6, "east": 98, "north": 37.5,
        "source": "NOT_A_REAL_SENSOR"
    })
    assert response.status_code == 400


def test_fires_endpoint_rejects_excessive_bounding_box():
    response = client.get("/api/v1/fires", params={
        "west": -170, "south": -80, "east": 170, "north": 80
    })
    assert response.status_code == 400
    assert "too large" in response.json()["detail"]


def test_fires_endpoint_rejects_antimeridian_box():
    response = client.get("/api/v1/fires", params={
        "west": 170, "south": -10, "east": -170, "north": 10
    })
    assert response.status_code == 400



async def fake_stale_observations(*args, **kwargs):
    raise StaleCacheError(SAMPLE, "2026-11-14T04:00:00+00:00")


def test_fires_endpoint_marks_stale_fallback(monkeypatch):
    monkeypatch.setattr(fires, "fetch_area_observations", fake_stale_observations)
    response = client.get("/api/v1/fires", params={
        "west": 68, "south": 6, "east": 98, "north": 37.5
    })
    assert response.status_code == 200
    body = response.json()
    assert body["data_status"] == "stale_cache"
    assert body["cached_at"] == "2026-11-14T04:00:00+00:00"
    assert body["count"] == 1
