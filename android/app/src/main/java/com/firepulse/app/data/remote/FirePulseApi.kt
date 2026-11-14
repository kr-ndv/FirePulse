package com.firepulse.app.data.remote
import retrofit2.http.GET
import retrofit2.http.Query
data class FireObservation(val latitude:Double,val longitude:Double,val acquisition_date:String,val acquisition_time:String?,val satellite:String?,val instrument:String?,val confidence:String?,val frp:Double?)
data class FireObservationsResponse(val source:String,val count:Int,val observations:List<FireObservation>)
interface FirePulseApi {
 @GET("api/v1/fires")
 suspend fun getFires(@Query("west") west:Double,@Query("south") south:Double,@Query("east") east:Double,@Query("north") north:Double,@Query("days") days:Int=1,@Query("source") source:String="VIIRS_SNPP_NRT"):FireObservationsResponse
}
