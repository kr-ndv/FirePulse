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
