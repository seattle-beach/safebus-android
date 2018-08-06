package io.pivotal.safebus

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import io.pivotal.safebus.api.BusStop

class MarkerOverlay(private val map: SafeBusMap,
                    private val iconResource: BusIconResource,
                    private val markerLimit: Int = 150) {

    private val markers: MutableSet<Marker> = HashSet()

    fun addStops(stops: List<BusStop>) {
        val newStops = stops
                .filter { stop ->
                    markers.none { marker -> isMarkerForStop(marker, stop) }
                }
                .take(markerLimit)

        if ((markers.size + newStops.size) > markerLimit) {
            markers.filterNot { m -> shouldRetain(m, stops) }.forEach(Marker::remove)
            markers.retainAll { m -> shouldRetain(m, stops) }
        }

        newStops.forEach { stop ->
            markers.add(map.addMarker(MarkerOptions()
                    .anchor(0.5f, 0.5f)
                    .title(stop.name)
                    .position(LatLng(stop.lat, stop.lon))
                    .icon(iconResource.getIcon(stop.direction))))
        }
    }

    private fun shouldRetain(marker: Marker, newStops: List<BusStop>): Boolean {
        return marker.isInfoWindowShown || newStops.any { isMarkerForStop(marker, it) }
    }

    private fun isMarkerForStop(marker: Marker, stop: BusStop): Boolean {
        return (marker.position.latitude == stop.lat
                && marker.position.longitude == stop.lon
                && marker.title == stop.name)
    }
}
