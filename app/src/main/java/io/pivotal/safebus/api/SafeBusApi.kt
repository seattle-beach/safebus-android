package io.pivotal.safebus.api

import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Query

interface SafeBusApi {

    @GET("api/bus_stops")
    fun findBusStops(@Query("lat") lat: Double,
                     @Query("lon") lon: Double,
                     @Query("lat_span") latSpan: Double,
                     @Query("lon_span") lonSpan: Double):
            Observable<List<BusStop>>
}
