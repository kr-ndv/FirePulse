package com.firepulse.app.ui.components

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.firepulse.app.data.remote.FireObservation
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

private const val FIRE_SOURCE_ID = "firepulse-observations"
private const val FIRE_LAYER_ID = "firepulse-hotspots"
private const val MAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"

@Composable
fun FireActivityMap(
    observations: List<FireObservation>,
    modifier: Modifier = Modifier,
    initialTarget: LatLng = LatLng(22.0, 79.0),
    initialZoom: Double = 4.2
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        MapLibre.getInstance(context.applicationContext)
        MapView(context).also { it.onCreate(null) }
    }
    val mapInstance = remember { androidx.compose.runtime.mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember { androidx.compose.runtime.mutableStateOf(false) }

    DisposableEffect(mapView, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_LOW_MEMORY -> mapView.onLowMemory()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxWidth().height(420.dp),
        factory = { mapView },
        update = { view ->
            if (mapInstance.value == null) {
                view.getMapAsync { map ->
                    mapInstance.value = map
                    map.cameraPosition = CameraPosition.Builder()
                        .target(initialTarget)
                        .zoom(initialZoom)
                        .build()
                    map.setStyle(MAP_STYLE) { style ->
                        if (style.getSource(FIRE_SOURCE_ID) == null) {
                            style.addSource(GeoJsonSource(FIRE_SOURCE_ID, FeatureCollection.fromFeatures(arrayOf())))
                        }
                        if (style.getLayer(FIRE_LAYER_ID) == null) {
                            style.addLayer(
                                CircleLayer(FIRE_LAYER_ID, FIRE_SOURCE_ID).withProperties(
                                    circleColor("#FF5722"),
                                    circleRadius(5.5f),
                                    circleOpacity(0.82f)
                                )
                            )
                        }
                        styleReady = true
                    }
                }
            }
        }
    )

    LaunchedEffect(observations, styleReady) {
        if (styleReady) {
            val features = observations.mapNotNull { fire ->
                val lat = fire.latitude
                val lon = fire.longitude
                if (lat !in -90.0..90.0 || lon !in -180.0..180.0) null
                else Feature.fromGeometry(Point.fromLngLat(lon, lat)).apply {
                    fire.satellite?.let { addStringProperty("satellite", it) }
                    fire.confidence?.let { addStringProperty("confidence", it) }
                    fire.frp?.let { addNumberProperty("frp", it) }
                    fire.acquisition_date?.let { addStringProperty("date", it) }
                }
            }
            val collection = FeatureCollection.fromFeatures(features)
            mapInstance.value?.style?.getSourceAs<GeoJsonSource>(FIRE_SOURCE_ID)?.setGeoJson(collection)
        }
    }
}
