package io.pivotal.safebus

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import io.ashdavies.rx.rxtasks.RxTasks
import io.pivotal.safebus.api.BusStop
import io.pivotal.safebus.api.SafeBusApi
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.zipWith
import io.reactivex.subjects.PublishSubject
import org.koin.android.ext.android.inject

class BusMapActivity : AppCompatActivity() {
    val LOCATION_PERMISSION_CODE = 0
    private val SAFEBUS_API_LIMIT = 50
    private val PIVOTAL_LOCATION = LatLng(47.5989794, -122.335976)

    private val grantedPermission = PublishSubject.create<Boolean>()

    private val safeBusApi by inject<SafeBusApi>()
    private val locationClient by inject<FusedLocationProviderClient>()
    private val mapEmitter by inject<MapEmitter>(parameters = { mapOf("activity" to this) })
    private val ioScheduler by inject<Scheduler>("io")
    private val uiScheduler by inject<Scheduler>("ui")

    private lateinit var map: SafeBusMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bus_map)

        setupMap()
        checkLocationPermission()
    }

    private fun setupMap() {
        val mapReady = mapEmitter.mapReady()
        val permissions = grantedPermission.firstOrError()
        val cameraStream = permissions
                .flatMap { allowed ->
                    if (allowed) {
                        getCurrentLocation().onErrorReturnItem(PIVOTAL_LOCATION)
                    } else {
                        Single.just(PIVOTAL_LOCATION)
                    }
                }
                .map { latLng -> CameraPosition.fromLatLngZoom(latLng, 16.0f) }

        mapEmitter.cameraIdle()
                .flatMap(this::fetchBusStopsInMap)
                .observeOn(uiScheduler)
                .subscribeBy(
                        onNext = { busStops -> map.markerOverlay.addStops(busStops) },
                        onError = { error -> Log.e("BusMapActivity", error.toString()) }
                )

        mapReady.zipWith(cameraStream)
                .subscribe { (map, camera) -> map.moveCamera(camera) }

        mapReady.zipWith(permissions)
                .subscribe { (map, locationAllowed) -> map.isMyLocationEnabled = locationAllowed }
    }

    private fun fetchBusStopsInMap(map: SafeBusMap): Observable<List<BusStop>> {
        this.map = map

        val center = map.latLngBounds.center
        val southwest = map.latLngBounds.southwest
        val northeast = map.latLngBounds.northeast
        val latSpan = northeast.latitude - southwest.latitude
        val lonSpan = northeast.longitude - southwest.longitude
        return safeBusApi.findBusStops(center.latitude, center.longitude, latSpan, lonSpan, SAFEBUS_API_LIMIT)
                .subscribeOn(ioScheduler)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == LOCATION_PERMISSION_CODE) {
            val grantedLocationPermission = permissions
                    .zip(grantResults.asIterable())
                    .any { (permission, result) ->
                        permission == Manifest.permission.ACCESS_FINE_LOCATION &&
                                result == PackageManager.PERMISSION_GRANTED
                    }

            grantedPermission.onNext(grantedLocationPermission)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation(): Single<LatLng> {
        return RxTasks.single(locationClient.lastLocation)
                .map { location -> LatLng(location.latitude, location.longitude) }
    }

    private fun checkLocationPermission() {
        val hasPermission = (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)

        if (!hasPermission) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_CODE)
        } else {
            grantedPermission.onNext(true)
        }
    }
}
