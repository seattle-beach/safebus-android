package io.pivotal.safebus

import android.annotation.SuppressLint
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class SafeBusMap(private val map: GoogleMap, iconResource: BusIconResource) {

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

    val markerOverlay = MarkerOverlay(this, iconResource)

    fun moveCamera(position: CameraPosition) {
        val cameraUpdate = CameraUpdateFactory.newCameraPosition(position)
        this.map.moveCamera(cameraUpdate)
    }

    fun addMarker(options: MarkerOptions): Marker = map.addMarker(options)
}