package io.pivotal.safebus

import android.support.v4.app.FragmentActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject

class MapEmitter(activity: FragmentActivity,
                 private val iconResource: BusIconResource,
                 private val favoriteStopsRepository: FavoriteStopsRepository) : OnMapReadyCallback {
    private val onMapReady = BehaviorSubject.create<SafeBusMap>()

    init {
        val mapFragment = activity.supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    fun mapReady(): Single<SafeBusMap> = onMapReady.firstOrError()

    override fun onMapReady(map: GoogleMap) {
        map.uiSettings.isRotateGesturesEnabled = false
        map.uiSettings.isTiltGesturesEnabled = false
        map.uiSettings.isZoomControlsEnabled = true

        val safeBusMap = SafeBusMap(map, iconResource, favoriteStopsRepository)
        onMapReady.onNext(safeBusMap)
    }
}