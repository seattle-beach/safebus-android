package io.pivotal.safebus

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.tbruyelle.rxpermissions2.RxPermissions
import io.ashdavies.rx.rxtasks.RxTasks
import io.pivotal.safebus.api.BusStop
import io.pivotal.safebus.api.SafeBusApi
import io.reactivex.Maybe
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.zipWith
import kotlinx.android.synthetic.main.activity_bus_map.*
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf

class BusMapActivity : AppCompatActivity() {
    enum class MapStatus {
        LOADING,
        UNSELECTED,
        SELECTED
    }

    private val PIVOTAL_LOCATION = LatLng(47.5989794, -122.335976)

    private val safeBusApi by inject<SafeBusApi>()
    private val locationClient by inject<FusedLocationProviderClient>()
    private val ioScheduler by inject<Scheduler>("io")
    private val uiScheduler by inject<Scheduler>("ui")
    private val rxPermissions by inject<RxPermissions> { parametersOf(this) }
    private val mapEmitter by inject<MapEmitter> { parametersOf(this) }
    private val favoriteStopsRepository by inject<FavoriteStopsRepository>()

    private lateinit var map: SafeBusMap

    private var selectedStop: SafeBusMarker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bus_map)

        val mapReady = initializeMap().doOnSuccess { this.map = it }

        val stops = mapReady
                .flatMapObservable { it.cameraIdle() }
                .switchMapMaybe { bounds ->
                    safeBusApi.stopsInside(bounds).subscribeOn(ioScheduler)
                }
                .observeOn(uiScheduler)

        mapReady.flatMapObservable { it.busStopTapped() }
                .subscribe { stop ->
                    selectedStop = stop //TODO: Revisit if we want to save it or ask SafeBusMap
                    val colorResource = iconColor(stop.isFavorite)
                    favoriteIcon.setColorFilter(getColor(colorResource))

                    busStopTitle.text = stop.name
                    toolBar.displayedChild = MapStatus.SELECTED.ordinal
                    map.animateCamera(CameraPosition.fromLatLngZoom(stop.position, map.cameraPosition.zoom))
                }

        stops.firstElement().subscribe { toolBar.displayedChild = MapStatus.UNSELECTED.ordinal }
        stops.subscribeBy { busStops -> map.addStops(busStops) }
    }

    fun favoriteClicked(_view: View) {
        selectedStop?.let {
            val added = favoriteStopsRepository.toggle(it.id)
            val colorResource = iconColor(added)
            favoriteIcon.setColorFilter(getColor(colorResource))
        }
    }

    private fun iconColor(isFavorite: Boolean): Int = if (isFavorite) {
        R.color.favoriteStop
    } else {
        R.color.notFavoriteStop
    }

    private fun initializeMap(): Single<SafeBusMap> {
        val mapReady = mapEmitter.mapReady()
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

        mapReady.zipWith(currentLocation).subscribe { (map, camera) -> map.moveCamera(camera) }

        return mapReady
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