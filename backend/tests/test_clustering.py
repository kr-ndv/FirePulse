import pytest

from app.services.clustering import cluster_observations


def observation(lat, lon, frp=10.0, confidence="h", day="2026-11-14", satellite="N"):
    return {
        "latitude": lat,
        "longitude": lon,
        "frp": frp,
        "confidence": confidence,
        "acquisition_date": day,
        "satellite": satellite,
    }


def test_nearby_detections_form_one_cluster():
    clusters = cluster_observations([
        observation(25.0, 78.0),
        observation(25.005, 78.005),
        observation(25.01, 78.01),
    ], radius_km=5)
    assert len(clusters) == 1
    assert clusters[0]["detection_count"] == 3
    assert clusters[0]["high_confidence_count"] == 3
    assert clusters[0]["frp_total_mw"] == 30.0


def test_distant_detections_remain_separate():
    clusters = cluster_observations([
        observation(25.0, 78.0),
        observation(26.0, 79.0),
    ], radius_km=5)
    assert len(clusters) == 2


def test_cluster_order_uses_heuristic_score():
    clusters = cluster_observations([
        observation(25.0, 78.0, frp=1, confidence="l"),
        observation(25.001, 78.001, frp=90, confidence="h"),
        observation(25.002, 78.002, frp=90, confidence="h"),
    ], radius_km=5)
    assert clusters[0]["severity_score"] >= clusters[-1]["severity_score"]


def test_radius_is_bounded():
    with pytest.raises(ValueError):
        cluster_observations([], radius_km=50)


def test_input_size_is_bounded():
    rows = [observation(25, 78)] * 5001
    with pytest.raises(ValueError):
        cluster_observations(rows)
