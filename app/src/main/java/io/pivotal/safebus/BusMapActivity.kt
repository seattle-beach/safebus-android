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
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.inject

class BusMapActivity : AppCompatActivity() {
    private val TAG = "BusMapActivity"

    private val LOCATION_PERMISSION_CODE = 0
    private val PIVOTAL_LOCATION = LatLng(47.5989794, -122.335976)

    lateinit var map: SafeBusMap
    val safeBusApi by inject<SafeBusApi>()
    val locationClient by inject<FusedLocationProviderClient>()
    val mapEmitter by inject<MapEmitter>(parameters = { mapOf("activity" to this) })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bus_map)


        mapEmitter.mapReady().subscribe({ t: SafeBusMap ->
            map = t

            val hasPermission = (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)

            if (hasPermission) {
                enableLocation()
            } else {
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_CODE)
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == LOCATION_PERMISSION_CODE) {
            val grantedLocationPermission = permissions
                    .zip(grantResults.asIterable())
                    .any { (permission, result) ->
                        permission == Manifest.permission.ACCESS_FINE_LOCATION &&
                                result == PackageManager.PERMISSION_GRANTED
                    }
            if (grantedLocationPermission) {
                enableLocation()
            } else {
                updateCamera(PIVOTAL_LOCATION)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableLocation() {
        map.isMyLocationEnabled = true
        locationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    markBusStopsOnMap()
                    if (location != null) {
                        val latlng = LatLng(location.latitude, location.longitude)
                        updateCamera(latlng)
                    } else {
                        updateCamera(PIVOTAL_LOCATION)
                    }
                }
    }

    fun updateCamera(latlng: LatLng) {
        map.moveCamera(CameraPosition.builder()
                .target(latlng)
                .zoom(15.0f)
                .build())
    }

    private fun markBusStopsOnMap() {
        safeBusApi.findBusStops(47.5989794, -122.335976, 0.01, 0.01)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ stops ->
                    System.out.println("GOT HERE WITH STOPS:" + stops.toString())
                })
//        // Instantiate the RequestQueue.
//        val queue = Volley.newRequestQueue(this)
//        val url = "https://safebus.cfapps.io/api/bus_stops?lat=47.5989794&lon=-122.335976&lat_span=0.01&lon_span=0.01"
//
//        val jsonObjectRequest = JsonArrayRequest(Request.Method.GET, url, null,
//                Response.Listener { response ->
//
//                    for (i in 0..(response.length() - 1)) {
//                        val stop = response.getJSONObject(i)
//
//                        val latLng = LatLng(stop.getDouble("lat"), stop.getDouble("lon"))
//
//                        map.addMarker(MarkerOptions().position(latLng)
//                                .title(stop.getString("name")))
//                    }
//                },
//                Response.ErrorListener {
//                    Log.e(TAG, "failed to call service")
//                })
//        queue.add(jsonObjectRequest)
    }

}
