package io.pivotal.safebus

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class BusMapActivity : AppCompatActivity(), OnMapReadyCallback {
    private val TAG = "BusMapActivity"

    private val LOCATION_PERMISSION_CODE = 0
    private val PIVOTAL_LOCATION = LatLng(47.5989794, -122.335976)

    lateinit var map: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bus_map)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

    }

    override fun onMapReady(googleMap: GoogleMap) {

        map = googleMap

        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_CODE)
        } else {
            enableLocation()
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
        fusedLocationProviderClient.lastLocation
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
        val zoomUpdate = CameraUpdateFactory.newLatLngZoom(latlng, 15.00f)
        map.animateCamera(zoomUpdate)
    }

    private fun markBusStopsOnMap() {
        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(this)
        val url = "https://safebus.cfapps.io/api/bus_stops?lat=47.5989794&lon=-122.335976&lat_span=0.01&lon_span=0.01"

        val jsonObjectRequest = JsonArrayRequest(Request.Method.GET, url, null,
                Response.Listener { response ->

                    for (i in 0..(response.length() - 1)) {
                        val stop = response.getJSONObject(i)

                        val latLng = LatLng(stop.getDouble("lat"), stop.getDouble("lon"))

                        map.addMarker(MarkerOptions().position(latLng)
                                .title(stop.getString("name")))
                    }
                },
                Response.ErrorListener {
                    Log.e(TAG, "failed to call service")
                })
        queue.add(jsonObjectRequest)
    }

}
