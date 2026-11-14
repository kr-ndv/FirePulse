"""Deterministic proximity clustering and heuristic activity scoring.

These clusters are analytical groupings of FIRMS thermal anomalies, not
validated fire perimeters or official incident/severity classifications.
"""
from collections import defaultdict
from math import ceil, cos, radians, asin, sqrt, sin

EARTH_RADIUS_KM = 6371.0088
MAX_CLUSTER_INPUT = 5000


def _distance_km(a: dict, b: dict) -> float:
    lat1, lon1 = radians(a["latitude"]), radians(a["longitude"])
    lat2, lon2 = radians(b["latitude"]), radians(b["longitude"])
    dlat, dlon = lat2 - lat1, lon2 - lon1
    h = sin(dlat / 2) ** 2
    h += cos(lat1) * cos(lat2) * sin(dlon / 2) ** 2
    return 2 * EARTH_RADIUS_KM * asin(sqrt(min(1.0, h)))


def _confidence_value(value) -> float:
    normalized = str(value or "").strip().lower()
    if normalized in {"h", "high"}:
        return 1.0
    if normalized in {"n", "nominal", "medium", "m"}:
        return 0.6
    if normalized in {"l", "low"}:
        return 0.25
    try:
        numeric = float(normalized)
        return max(0.0, min(1.0, numeric / 100.0))
    except (TypeError, ValueError):
        return 0.0


def _severity(count: int, frp_total: float, high_fraction: float):
    # Heuristic only: density 40%, summed FRP 40%, high-confidence share 20%.
    score = round(
        min(count / 10.0, 1.0) * 40
        + min(frp_total / 100.0, 1.0) * 40
        + max(0.0, min(high_fraction, 1.0)) * 20,
        1,
    )
    label = "low" if score < 25 else "moderate" if score < 50 else "elevated" if score < 75 else "high"
    return score, label


def cluster_observations(observations: list[dict], radius_km: float = 5.0) -> list[dict]:
    if not 0.5 <= radius_km <= 25:
        raise ValueError("radius_km must be between 0.5 and 25")
    if len(observations) > MAX_CLUSTER_INPUT:
        raise ValueError(f"Too many observations to cluster; maximum is {MAX_CLUSTER_INPUT}")

    points = [
        row for row in observations
        if isinstance(row.get("latitude"), (int, float))
        and isinstance(row.get("longitude"), (int, float))
        and -90 <= row["latitude"] <= 90
        and -180 <= row["longitude"] <= 180
    ]
    if not points:
        return []

    # Spatial buckets avoid a full all-pairs comparison for normal datasets.
    cell_deg = radius_km / 111.32
    buckets = defaultdict(list)
    parent = list(range(len(points)))

    def find(x):
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x

    def union(a, b):
        ra, rb = find(a), find(b)
        if ra != rb:
            parent[rb] = ra

    for index, point in enumerate(points):
        lat, lon = point["latitude"], point["longitude"]
        y = int((lat + 90) / cell_deg)
        x = int((lon + 180) / cell_deg)
        lon_span = min(180, int(ceil(1 / max(cos(radians(lat)), 0.01))))
        for by in range(y - 1, y + 2):
            for bx in range(x - lon_span, x + lon_span + 1):
                for other in buckets.get((by, bx), []):
                    if _distance_km(point, points[other]) <= radius_km:
                        union(index, other)
        buckets[(y, x)].append(index)

    grouped = defaultdict(list)
    for index, point in enumerate(points):
        grouped[find(index)].append(point)

    clusters = []
    for members in grouped.values():
        count = len(members)
        frp_values = [float(row.get("frp") or 0) for row in members]
        frp_total = round(sum(max(0.0, value) for value in frp_values), 2)
        high_count = sum(_confidence_value(row.get("confidence")) >= 0.9 for row in members)
        score, label = _severity(count, frp_total, high_count / count)
        dates = sorted({str(row.get("acquisition_date")) for row in members if row.get("acquisition_date")})
        satellites = sorted({str(row.get("satellite") or "Unknown") for row in members})
        clusters.append({
            "latitude": round(sum(row["latitude"] for row in members) / count, 6),
            "longitude": round(sum(row["longitude"] for row in members) / count, 6),
            "detection_count": count,
            "frp_total_mw": frp_total,
            "high_confidence_count": high_count,
            "severity_score": score,
            "severity_label": label,
            "first_acquisition_date": dates[0] if dates else None,
            "last_acquisition_date": dates[-1] if dates else None,
            "satellites": satellites,
        })
    clusters.sort(key=lambda item: (item["severity_score"], item["detection_count"]), reverse=True)
    for number, cluster in enumerate(clusters, start=1):
        cluster["cluster_id"] = number
    return clusters
