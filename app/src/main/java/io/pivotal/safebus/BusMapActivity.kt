package io.pivotal.safebus

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.tbruyelle.rxpermissions2.RxPermissions
import io.ashdavies.rx.rxtasks.RxTasks
import io.pivotal.safebus.api.BusStop
import io.pivotal.safebus.api.SafeBusApi
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.zipWith
import org.koin.android.ext.android.inject

class BusMapActivity : AppCompatActivity() {
    private val PIVOTAL_LOCATION = LatLng(47.5989794, -122.335976)

    private val safeBusApi by inject<SafeBusApi>()
    private val locationClient by inject<FusedLocationProviderClient>()
    private val ioScheduler by inject<Scheduler>("io")
    private val uiScheduler by inject<Scheduler>("ui")
    private val rxPermissions by inject<RxPermissions>(parameters = { mapOf("activity" to this) })
    private val mapEmitter by inject<MapEmitter>(parameters = { mapOf("activity" to this) })

    private lateinit var map: SafeBusMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bus_map)

        initializeMap()

        // Search for Bus Stops whenever the SafeBusMap moves
        mapEmitter.cameraIdle()
                .zipSwitch { map -> safeBusApi.stopsInside(map.latLngBounds).subscribeOn(ioScheduler) }
                .observeOn(uiScheduler)
                .subscribeBy(
                        onNext = { (map, busStops) -> map.markerOverlay.addStops(busStops) },
                        onError = { error -> Log.e("BusMapActivity", error.toString()) }
                )
    }

    private fun initializeMap() {
        val mapReady = mapEmitter.mapReady().doOnSuccess { this.map = it }
        val locationGranted = hasLocationPermission()

        // set `myLocationEnabled` map layer based on location permission
        mapReady.zipWith(locationGranted)
                .subscribe { (map, granted) -> map.isMyLocationEnabled = granted }

        // move map to current location
        val currentLocation = locationGranted
                .filter { it }
                .flatMapSingleElement { currentLocation() }
                .toSingle()
                .onErrorReturnItem(PIVOTAL_LOCATION)
                .map { CameraPosition.fromLatLngZoom(it, 16.0f) }

        mapReady.zipWith(currentLocation)
                .subscribe { (map, camera) -> map.moveCamera(camera) }
    }

    private fun hasLocationPermission(): Single<Boolean> = rxPermissions
            .request(ACCESS_FINE_LOCATION)
            .filter { it }
            .first(false)

    @SuppressLint("MissingPermission")
    private fun currentLocation(): Single<LatLng> = RxTasks
            .single(locationClient.lastLocation)
            .map { location -> LatLng(location.latitude, location.longitude) }
}

private fun SafeBusApi.stopsInside(bounds: LatLngBounds): Maybe<List<BusStop>> {
    val SAFEBUS_API_LIMIT = 50

    val center = bounds.center
    val southwest = bounds.southwest
    val northeast = bounds.northeast
    val latSpan = northeast.latitude - southwest.latitude
    val lonSpan = northeast.longitude - southwest.longitude
    return findBusStops(center.latitude, center.longitude, latSpan, lonSpan, SAFEBUS_API_LIMIT)
            .firstElement()
            .doOnError { error -> Log.e("SafeBusApi::stopsInside", error.toString()) }
            .onErrorResumeNext(Maybe.empty())
}

// cancels previous request from source (`this`) during the next event
// creates a tuple of source + output
private fun <T, U> Observable<T>.zipSwitch(mapper: (T) -> Maybe<U>): Observable<Pair<T, U>> =
        this.switchMapMaybe { source -> Maybe.just(source).zipWith(mapper(source)) }