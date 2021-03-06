package io.pivotal.safebus

import android.annotation.SuppressLint
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import io.pivotal.safebus.api.BusStop
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

class SafeBusMap(private val map: GoogleMap, iconResource: BusIconResource, favoriteStopsRepository: FavoriteStopsRepository) {
    private val onCameraIdle = BehaviorSubject.create<LatLngBounds>()

    var isMyLocationEnabled: Boolean
        get() = this.map.isMyLocationEnabled
        @SuppressLint("MissingPermission")
        set(flag) {
            this.map.isMyLocationEnabled = flag
        }

    val cameraPosition: CameraPosition
        get() = this.map.cameraPosition

    val latLngBounds: LatLngBounds
        get() = this.map.projection.visibleRegion.latLngBounds

    private val markerOverlay = MarkerOverlay(this, iconResource, favoriteStopsRepository)

    init {
        map.setOnCameraIdleListener { onCameraIdle.onNext(this.latLngBounds) }
        map.setOnMarkerClickListener { marker ->
            this.markerOverlay.onMarkerClicked.onNext(marker)
            true
        }
    }

    fun cameraIdle(): Observable<LatLngBounds> = onCameraIdle

    fun busStopTapped(): Observable<SafeBusMarker> = this.markerOverlay.busStopTapped()

    fun moveCamera(position: CameraPosition) = this.map.moveCamera(CameraUpdateFactory.newCameraPosition(position))

    fun animateCamera(position: CameraPosition) = this.map.animateCamera(CameraUpdateFactory.newCameraPosition(position))

    fun addStops(stops: List<BusStop>) = markerOverlay.addStops(stops)

    // used by the overlay
    fun addMarker(options: MarkerOptions): Marker = map.addMarker(options)
}