package io.pivotal.safebus

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
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

// cancels previous request from source (`this`) during the next event
// creates a tuple of source + output
private fun <T, U> Observable<T>.zipSwitch(mapper: (T) -> Maybe<U>): Observable<Pair<T, U>> =
        this.switchMapMaybe { source -> Maybe.just(source).zipWith(mapper(source)) }

class BusMapActivity : AppCompatActivity() {
    private val SAFEBUS_API_LIMIT = 50
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
                .zipSwitch(this::fetchBusStopsInMap)
                .observeOn(uiScheduler)
                .subscribeBy(
                        onNext = { (map, busStops) -> map.markerOverlay.addStops(busStops) },
                        onError = { error -> Log.e("BusMapActivity", error.toString()) }
                )
    }

    @SuppressLint("MissingPermission")
    private fun initializeMap() {
        val mapReady = mapEmitter.mapReady().doOnSuccess { this.map = it }
        val locationGranted = hasLocationPermission()

        // set `myLocationEnabled` map layer based on location permission
        mapReady.zipWith(locationGranted)
                .subscribe { (map, granted) -> map.isMyLocationEnabled = granted }

        // move map to current location
        val currentLocation = locationGranted
                .filter { it }
                .flatMapSingleElement { RxTasks.single(locationClient.lastLocation) }
                .map { location -> LatLng(location.latitude, location.longitude) }
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

    private fun fetchBusStopsInMap(map: SafeBusMap): Maybe<List<BusStop>> {
        val center = map.latLngBounds.center
        val southwest = map.latLngBounds.southwest
        val northeast = map.latLngBounds.northeast
        val latSpan = northeast.latitude - southwest.latitude
        val lonSpan = northeast.longitude - southwest.longitude
        return safeBusApi
                .findBusStops(center.latitude, center.longitude, latSpan, lonSpan, SAFEBUS_API_LIMIT)
                .firstElement()
                .doOnError { error -> Log.e("BusMapActivity", error.toString()) }
                .onErrorResumeNext(Maybe.empty())
                .subscribeOn(ioScheduler)
    }
}
