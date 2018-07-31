package io.pivotal.safebus

import android.annotation.SuppressLint
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
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

    fun moveCamera(position: CameraPosition) {
        val cameraUpdate = CameraUpdateFactory.newCameraPosition(position)
        this.map.animateCamera(cameraUpdate)
    }

    fun addBusStop(busStop: BusStop) {
        map.addMarker(
                MarkerOptions()
                        .position(LatLng(busStop.lat, busStop.lon))
                        .title(busStop.name)
        )
    }
}