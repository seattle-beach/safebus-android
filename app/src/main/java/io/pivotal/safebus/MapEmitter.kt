package io.pivotal.safebus

import android.support.v4.app.FragmentActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject

class MapEmitter(activity: FragmentActivity, private val iconResource: BusIconResource) : OnMapReadyCallback, GoogleMap.OnCameraIdleListener {
    private val onMapReady = BehaviorSubject.create<SafeBusMap>()
    private val onCameraIdle = BehaviorSubject.create<SafeBusMap>()

    init {
        val mapFragment = activity.supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    fun mapReady(): Single<SafeBusMap> {
        return onMapReady.firstOrError()
    }

    fun cameraIdle(): Observable<SafeBusMap> {
        return onCameraIdle
    }

    override fun onMapReady(map: GoogleMap) {
        map.setOnCameraIdleListener(this)
        map.uiSettings.isRotateGesturesEnabled = false
        map.uiSettings.isTiltGesturesEnabled = false
        map.uiSettings.isZoomControlsEnabled = true
        onMapReady.onNext(SafeBusMap(map, iconResource))
    }

    override fun onCameraIdle() {
        mapReady().subscribe(onCameraIdle::onNext)
    }
}