package io.pivotal.safebus

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import io.pivotal.safebus.api.BusStop
import io.pivotal.safebus.extensions.rx.mapNotNull
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import java.util.*
import kotlin.math.pow

class MarkerOverlay(private val map: SafeBusMap,
                    private val iconResource: BusIconResource,
                    private val markerLimit: Int = 150) {

    private val busStops = HashMap<Marker, BusStop>()
    private var tappedMarker: Marker? = null
    val onMarkerClicked = BehaviorSubject.create<Marker>()

    fun addStops(stops: Iterable<BusStop>) {
        var (newStops, alreadyAdded) = stops.partition(this::isNewStop)
        newStops = newStops.take(markerLimit)

        // remove extra markers
        val extraMarkerCount = busStops.size + newStops.size - markerLimit
        extraMarkers(alreadyAdded, extraMarkerCount).forEach { marker ->
            marker.remove()
            busStops.remove(marker)
        }

        newStops.map { stop -> Pair(stop.into(), stop) }
                .map { (markerOptions, stop) -> Pair(map.addMarker(markerOptions), stop) }
                .let { newMarkers -> busStops.putAll(newMarkers) }
    }

    fun busStopTapped(): Observable<BusStop> = onMarkerClicked
            .mapNotNull { busStops[it] }
            .doOnNext {
                tappedMarker?.remove()
                tappedMarker = map.addMarker(MarkerOptions().position(LatLng(it.lat, it.lon)))
            }

    private fun BusStop.into(): MarkerOptions {
        return MarkerOptions()
                .anchor(0.5f, 0.5f)
                .title(this.name)
                .position(LatLng(this.lat, this.lon))
                .icon(iconResource.getIcon(this.direction))
    }

    private fun extraMarkers(alreadyAdded: List<BusStop>, extraMarkerCount: Int): List<Marker> {
        return if (extraMarkerCount > 0) {
            busStops
                    .filterNot { (marker, stop) ->
                        marker.isInfoWindowShown || alreadyAdded.contains(stop)
                    }
                    .keys
                    .sortedByDescending(this::distance) //remove the ones farther from the center first
                    .take(extraMarkerCount) //but only remove up to the extraMarkerCount
        } else {
            emptyList()
        }
    }

    private fun distance(m1: Marker): Double {
        val marker = m1.position
        val map = map.latLngBounds.center
        return (marker.latitude - map.latitude).pow(2) + (marker.longitude - map.longitude).pow(2)
    }

    private fun isNewStop(stop: BusStop) = !busStops.values.contains(stop)
}