package io.pivotal.safebus

import android.annotation.SuppressLint
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import io.pivotal.safebus.api.BusStop

class SafeBusMap(val map: GoogleMap) {

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

    val markerOverlay = MarkerOverlay(this)

    fun moveCamera(position: CameraPosition) {
        val cameraUpdate = CameraUpdateFactory.newCameraPosition(position)
        this.map.animateCamera(cameraUpdate)
    }

    fun addMarker(busStop: BusStop): Marker = map.addMarker(MarkerOptions()
            .position(LatLng(busStop.lat, busStop.lon))
            .title(busStop.name)
    )
}