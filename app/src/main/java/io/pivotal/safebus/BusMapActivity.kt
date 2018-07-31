package io.pivotal.safebus

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import io.pivotal.safebus.api.SafeBusApi
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.koin.android.ext.android.inject

class BusMapActivity : AppCompatActivity() {
    private val TAG = "BusMapActivity"

    private val LOCATION_PERMISSION_CODE = 0
    private val PIVOTAL_LOCATION = LatLng(47.5989794, -122.335976)
    private val grantedPermission = PublishSubject.create<Boolean>()

    lateinit var map: SafeBusMap
    val safeBusApi by inject<SafeBusApi>()
    val locationClient by inject<FusedLocationProviderClient>()
    val mapEmitter by inject<MapEmitter>(parameters = { mapOf("activity" to this) })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bus_map)

        val mapReady = mapEmitter.mapReady()

        val permissions = grantedPermission
        permissions.zipWith(mapReady, BiFunction<Boolean, SafeBusMap, Unit>{ permission, map ->
            map.isMyLocationEnabled = permission
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()

        val location = grantedPermission.flatMap { permission ->
            if (permission) {
                getCurrentLocation()
            } else {
                Observable.just(PIVOTAL_LOCATION)
            }
        }

        mapReady.zipWith(location, BiFunction<SafeBusMap, LatLng, Unit> { map, latLng ->
            map.moveCamera(CameraPosition.fromLatLngZoom(latLng, 15.0f))
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()).subscribe()

        checkLocationPermission()
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation(): PublishSubject<LatLng>? {
        val locationStream = PublishSubject.create<LatLng>()
        locationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val latlng = LatLng(location.latitude, location.longitude)
                locationStream.onNext(latlng)
            } else {
                locationStream.onNext(PIVOTAL_LOCATION)
            }
        }
        return locationStream
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
}
