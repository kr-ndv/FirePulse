package com.firepulse.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

data class FireObservation(
    val latitude: Double,
    val longitude: Double,
    val acquisition_date: String,
    val acquisition_time: String?,
    val satellite: String?,
    val instrument: String?,
    val confidence: String?,
    val frp: Double?,
    val brightness: Double?,
    val daynight: String?
)

data class FireObservationsResponse(
    val source: String,
    val count: Int,
    val retrieved_on: String?,
    val observations: List<FireObservation>,
    val data_status: String? = "live",
    val cached_at: String? = null
)

data class FireCluster(
    val cluster_id: Int,
    val latitude: Double,
    val longitude: Double,
    val detection_count: Int,
    val frp_total_mw: Double,
    val high_confidence_count: Int,
    val severity_score: Double,
    val severity_label: String,
    val first_acquisition_date: String?,
    val last_acquisition_date: String?,
    val satellites: List<String>
)

data class FireClustersResponse(
    val source: String,
    val days: Int,
    val radius_km: Double,
    val detection_count: Int,
    val cluster_count: Int,
    val data_status: String,
    val cached_at: String?,
    val scoring_method: String,
    val clusters: List<FireCluster>
)

data class DailyFireCount(val date: String, val detections: Int)
data class SatelliteFireCount(val satellite: String, val detections: Int)

data class HistoricalFireSummaryResponse(
    val source: String,
    val days: Int,
    val start_date: String,
    val end_date: String,
    val count: Int,
    val data_status: String,
    val cached_at: String?,
    val by_day: List<DailyFireCount>,
    val by_satellite: List<SatelliteFireCount>,
    val frp_total_mw: Double,
    val high_confidence_count: Int,
    val note: String
)

data class FireSummaryResponse(
    val source: String,
    val days: Int,
    val count: Int,
    val retrieved_on: String,
    val by_day: List<DailyFireCount>,
    val by_satellite: List<SatelliteFireCount>,
    val frp_total_mw: Double,
    val high_confidence_count: Int,
    val data_status: String? = "live",
    val cached_at: String? = null
)

interface FirePulseApi {
    @GET("api/v1/fires")
    suspend fun getFires(
        @Query("west") west: Double,
        @Query("south") south: Double,
        @Query("east") east: Double,
        @Query("north") north: Double,
        @Query("days") days: Int = 1,
        @Query("source") source: String = "VIIRS_SNPP_NRT"
    ): FireObservationsResponse

    @GET("api/v1/clusters")
    suspend fun getClusters(
        @Query("west") west: Double,
        @Query("south") south: Double,
        @Query("east") east: Double,
        @Query("north") north: Double,
        @Query("days") days: Int = 5,
        @Query("source") source: String = "VIIRS_SNPP_NRT",
        @Query("radius_km") radiusKm: Double = 5.0
    ): FireClustersResponse

    @GET("api/v1/history")
    suspend fun getHistoricalSummary(
        @Query("west") west: Double,
        @Query("south") south: Double,
        @Query("east") east: Double,
        @Query("north") north: Double,
        @Query("start_date") startDate: String,
        @Query("days") days: Int = 5,
        @Query("source") source: String = "VIIRS_SNPP_SP"
    ): HistoricalFireSummaryResponse

    @GET("api/v1/summary")
    suspend fun getSummary(
        @Query("west") west: Double,
        @Query("south") south: Double,
        @Query("east") east: Double,
        @Query("north") north: Double,
        @Query("days") days: Int = 5,
        @Query("source") source: String = "VIIRS_SNPP_NRT"
    ): FireSummaryResponse
}
