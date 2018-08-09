package io.pivotal.safebus

import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import io.pivotal.safebus.api.BusStop
import io.pivotal.safebus.api.Direction
import io.pivotal.safebus.extensions.rx.mapNotNull
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kotlin.math.pow

class MarkerOverlay(private val map: SafeBusMap,
                    private val iconResource: BusIconResource,
                    private val favoriteStopsRepository: FavoriteStopsRepository,
                    private val markerLimit: Int = 150) {

    private val safeBusMarkers: MutableSet<SafeBusMarker> = mutableSetOf()
    private var tappedMarker: Pair<Marker, SafeBusMarker>? = null
    val onMarkerClicked = BehaviorSubject.create<Marker>()

    init {
        favoriteStopsRepository.onToggle().subscribe { (stopId, isFavorite) ->
            val marker = safeBusMarkers.find { it.id == stopId }
            marker?.also {
                it.isFavorite = isFavorite
                it.setIcon(iconResource.getIcon(it.direction, it.isFavorite))
            }
        }
    }

    fun addStops(stops: Iterable<BusStop>) {
        val allStops = stops.toMutableList()
        val alreadyAdded = removeExisting(allStops)
        val newStops = allStops.take(markerLimit)

        // remove extra markers
        val extraMarkerCount = safeBusMarkers.size + newStops.size - markerLimit
        extraMarkers(alreadyAdded, extraMarkerCount).forEach { safeBusMarker ->
            safeBusMarker.remove()
            safeBusMarkers.remove(safeBusMarker)
        }

        newStops
                .map { stop ->
                    val isFavorite = favoriteStopsRepository.isFavorite(stop.id)
                    val markerOptions = stop.into(isFavorite)
                    SafeBusMarker(
                            stop = stop,
                            googleMarker = map.addMarker(markerOptions)
                    ).also { it.isFavorite = isFavorite }
                }
                .let { newMarkers -> safeBusMarkers.addAll(newMarkers) }
    }

    fun busStopTapped(): Observable<SafeBusMarker> = onMarkerClicked
            .mapNotNull { googleMarker -> safeBusMarkers.find { safeMarker -> safeMarker.isWrapperOf(googleMarker) } }
            .doOnNext { tappedMarker ->
                //undo previous tapped if existing
                this.tappedMarker?.also { (googleMarker, safeBusMarker) ->
                    googleMarker.remove()
                    safeBusMarker.isSelected = false
                }
                tappedMarker.isSelected = true
                val googleMarker = map.addMarker(MarkerOptions().position(tappedMarker.position))
                this.tappedMarker = Pair(googleMarker, tappedMarker)
            }

    private fun BusStop.into(isFavorite: Boolean): MarkerOptions {
        return MarkerOptions()
                .anchor(0.5f, 0.5f)
                .title(name)
                .position(LatLng(lat, lon))
                .icon(iconResource.getIcon(direction, isFavorite))
    }

    private fun extraMarkers(reAdded: Set<SafeBusMarker>, extraMarkerCount: Int): List<SafeBusMarker> {
        return if (extraMarkerCount > 0) {
            val notToRemove = tappedMarker?.let { reAdded + it.second } ?: reAdded
            (safeBusMarkers - notToRemove)
                    .sortedByDescending(this::distance) //remove the ones farther from the center first
                    .take(extraMarkerCount) //but only remove up to the extraMarkerCount
        } else {
            emptyList()
        }
    }

    private fun distance(m1: SafeBusMarker): Double {
        val marker = m1.position
        val map = map.latLngBounds.center
        return (marker.latitude - map.latitude).pow(2) + (marker.longitude - map.longitude).pow(2)
    }

    private fun removeExisting(stops: MutableList<BusStop>): Set<SafeBusMarker> {
        val alreadyAdded: MutableSet<SafeBusMarker> = mutableSetOf()

        stops.removeIf { stop ->
            val existingMarker = safeBusMarkers.find { safeBusMarker -> safeBusMarker.id == stop.id }
            if (existingMarker != null) {
                alreadyAdded.add(existingMarker)
                true
            } else {
                false
            }
        }

        return alreadyAdded
    }
}

data class SafeBusMarker(private val stop: BusStop, private val googleMarker: Marker) {
    val name: String
        get() = stop.name

    val position: LatLng
        get() = LatLng(stop.lat, stop.lon)

    val id: String
        get() = stop.id

    val direction: Direction
        get() = stop.direction

    // do not include these in constructor so that it does not mess with equals/hashcode
    var isSelected: Boolean = false
    var isFavorite: Boolean = false

    fun remove() = googleMarker.remove()
    fun isWrapperOf(googleMarker: Marker): Boolean = googleMarker == this.googleMarker
    fun setIcon(icon: BitmapDescriptor) = this.googleMarker.setIcon(icon)
}