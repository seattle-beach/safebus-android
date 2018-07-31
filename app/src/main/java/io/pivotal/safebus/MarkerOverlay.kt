package io.pivotal.safebus

import com.google.android.gms.maps.model.Marker
import io.pivotal.safebus.api.BusStop

class MarkerOverlay(val map: SafeBusMap, private val markerLimit: Int = 50) {

    private val markers: MutableSet<Marker> = HashSet()

    fun addStops(stops: List<BusStop>) {

        if ((markers.size + stops.size) > markerLimit) {
            markers.forEach(Marker::remove)
            markers.clear()
        }

        val uniqueStops = stops
                .filter { stop ->
                    markers.none { marker ->
                        marker.position.latitude == stop.lat
                                && marker.position.longitude == stop.lon
                                && marker.title == stop.name
                    }
                }
                .take(markerLimit)

        uniqueStops.forEach { stop ->
            markers.add(map.addMarker(stop))
        }
    }
}
