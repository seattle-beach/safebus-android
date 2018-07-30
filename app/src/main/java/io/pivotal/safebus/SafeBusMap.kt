package io.pivotal.safebus

import android.annotation.SuppressLint
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition

class SafeBusMap(val map: GoogleMap) {

    var isMyLocationEnabled: Boolean
        get() = this.map.isMyLocationEnabled
        @SuppressLint("MissingPermission")
        set(flag) {
            this.map.isMyLocationEnabled = flag
        }

    fun moveCamera(position: CameraPosition) {
        val cameraUpdate = CameraUpdateFactory.newCameraPosition(position)
        this.map.animateCamera(cameraUpdate)
    }
}