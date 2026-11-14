from collections import defaultdict

def daily_sensor_counts(observations: list[dict]) -> list[dict]:
    """Aggregate by acquisition date and sensor without merging sensor counts."""
    counts = defaultdict(int)
    for item in observations:
        day = item.get("acquisition_date")
        sensor = item.get("sensor") or item.get("satellite") or "unknown"
        if day:
            counts[(day, sensor)] += 1
    return [{"date": day, "sensor": sensor, "detections": count}
            for (day, sensor), count in sorted(counts.items())]

def spatial_cell(latitude: float, longitude: float, cell_degrees: float = 0.05) -> str:
    """Prototype degree grid; not equal-area and not suitable for area comparisons."""
    if cell_degrees <= 0:
        raise ValueError("cell_degrees must be positive")
    return f"{int(latitude // cell_degrees)}:{int(longitude // cell_degrees)}"
