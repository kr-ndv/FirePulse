from app.harmonization import daily_sensor_counts, spatial_cell

def test_daily_sensor_counts_preserve_sensor():
    rows = [
        {"acquisition_date": "2026-11-14", "satellite": "N"},
        {"acquisition_date": "2026-11-14", "satellite": "N"},
        {"acquisition_date": "2026-11-14", "satellite": "J"},
    ]
    result = daily_sensor_counts(rows)
    assert result == [
        {"date": "2026-11-14", "sensor": "J", "detections": 1},
        {"date": "2026-11-14", "sensor": "N", "detections": 2},
    ]

def test_spatial_cell_is_stable():
    assert spatial_cell(12.34, 77.56, 0.05) == "246:1551"
