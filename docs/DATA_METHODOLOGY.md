# Data methodology

FirePulse uses NASA FIRMS active fire observations. A hotspot is a satellite thermal-anomaly detection; it is not equivalent to a confirmed fire perimeter or burned-area measurement.

## Harmonization principles
1. Preserve the originating sensor and product on every observation.
2. Normalize acquisition date and time consistently; use UTC for cross-sensor daily grouping.
3. Keep MODIS and VIIRS counts separately visible.
4. Aggregate observations to a documented common spatial grid for comparison.
5. Do not treat raw counts from different sensors as directly interchangeable.
6. Distinguish missing observations from a measured zero.
7. Record source product, retrieval time and near-real-time/standard product status.

The initial degree-grid helper is a prototype only. Scientific area comparisons across latitude require a suitable projected/equal-area grid and documented cell definition.

## Potential fire-activity clusters

The prototype clusters FIRMS detections using great-circle distance and a configurable neighborhood radius (default 5 km). Connected-component grouping is used: detections A and C may be in one cluster through B even when A and C are farther apart than the radius. Cluster centroids are arithmetic means of member coordinates, not fire perimeters or area estimates.

Each cluster reports detection count, summed reported FRP, high-confidence count, acquisition-date span, contributing satellite labels and a 0–100 heuristic score. The score is a transparent prototype ranking:
- Detection count: up to 40 points, saturating at 10 detections.
- Summed FRP: up to 40 points, saturating at 100 MW.
- Share of high-confidence detections: up to 20 points.

Labels are low (<25), moderate (25–49.9), elevated (50–74.9) and high (75–100). These labels are FirePulse heuristic bands only. They are not official fire danger ratings, validated incident severity, spread probabilities or emergency warnings. The score does not model weather, wind, fuel, terrain, population exposure, active perimeter, sensor sampling bias or satellite overpass timing.

The endpoint accepts at most 5,000 observations and radii from 0.5 to 25 km. Clusters can combine observations from multiple dates and sensors within the requested data window; the date span and satellite list are returned so clients can communicate that context. Raw MODIS and VIIRS detections are not necessarily equivalent measurements.
