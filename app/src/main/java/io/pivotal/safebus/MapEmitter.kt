package io.pivotal.safebus

import android.support.v4.app.FragmentActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

class MapEmitter(activity: FragmentActivity) : OnMapReadyCallback {
    private val emitter = BehaviorSubject.create<SafeBusMap>()

    init {
        val mapFragment = activity.supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        emitter.onNext(SafeBusMap(map))
    }

    fun mapReady(): Observable<SafeBusMap> {
        return emitter
    }

}