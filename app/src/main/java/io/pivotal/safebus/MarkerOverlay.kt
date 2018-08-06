package io.pivotal.safebus

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import io.pivotal.safebus.api.BusStop
import kotlin.math.pow

class MarkerOverlay(private val map: SafeBusMap,
                    private val iconResource: BusIconResource,
                    private val markerLimit: Int = 150) {

    private val markers: MutableSet<Marker> = HashSet()

    fun addStops(stops: Iterable<BusStop>) {
        val newStops = stops.filter(this::isNewStop).take(markerLimit)

        val extraMarkerCount = markers.size + newStops.size - markerLimit
        if (extraMarkerCount > 0) {
            val extraMarkers = markers.filterNot { m -> shouldRetain(m, stops) } // do not remove the active marker nor the new markers
                    .sortedByDescending(this::distance) //remove the ones farther from the center first
                    .take(extraMarkerCount) //but only remove up to the extraMarkerCount

            extraMarkers.forEach(Marker::remove)
            markers.removeAll(extraMarkers)
        }

        newStops.forEach { stop ->
            markers.add(map.addMarker(MarkerOptions()
                    .anchor(0.5f, 0.5f)
                    .title(stop.name)
                    .position(LatLng(stop.lat, stop.lon))
                    .icon(iconResource.getIcon(stop.direction))))
        }
    }

    private fun distance(m1: Marker): Double {
        val marker = m1.position
        val map = map.latLngBounds.center
        return (marker.latitude - map.latitude).pow(2) + (marker.longitude - map.longitude).pow(2)
    }

    private fun isNewStop(stop: BusStop) = markers.none { marker -> isMarkerForStop(marker, stop) }

    private fun shouldRetain(marker: Marker, newStops: Iterable<BusStop>): Boolean {
        return marker.isInfoWindowShown || newStops.any { isMarkerForStop(marker, it) }
    }

    private fun isMarkerForStop(marker: Marker, stop: BusStop): Boolean {
        return (marker.position.latitude == stop.lat
                && marker.position.longitude == stop.lon
                && marker.title == stop.name)
    }
}
