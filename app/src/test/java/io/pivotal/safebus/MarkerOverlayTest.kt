package io.pivotal.safebus

import com.google.android.gms.maps.model.*
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.pivotal.safebus.api.BusStop
import io.pivotal.safebus.api.Direction
import org.junit.Before
import org.junit.Test

class MarkerOverlayTest {

    @MockK
    private lateinit var safeBusMap: SafeBusMap
    @MockK
    private lateinit var iconResource: BusIconResource

    private lateinit var capturedMarkers: MutableMap<String, Marker>
    private lateinit var capturedIcons: MutableMap<Direction, BitmapDescriptor>

    private lateinit var subject: MarkerOverlay

    private val northStop = BusStop("James St. 1", 47.599274, -122.333282, Direction.NORTH)
    private val southStop = BusStop("James St. 2", 48.599274, -122.333282, Direction.SOUTH)
    private val noDirectionStop = BusStop("James St. 3", 49.599274, -122.333282, Direction.NONE)

    private fun MockKVerificationScope.markerStopMatcher(firstStop: BusStop): MarkerOptions =
            match { m -> m.isMarkerForStop(firstStop) }

    private fun MarkerOptions.isMarkerForStop(stop: BusStop): Boolean {
        return this.position.latitude == stop.lat
                && this.position.longitude == stop.lon
                && this.title == stop.name
                && this.icon == capturedIcons[stop.direction]
                && this.anchorU == 0.5f
                && this.anchorV == 0.5f
                && this.icon != null
    }

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        capturedMarkers = HashMap()
        capturedIcons = HashMap()

        val markerCapture = slot<MarkerOptions>()
        every { safeBusMap.addMarker(capture(markerCapture)) } answers {
            val marker = mockk<Marker>(relaxUnitFun = true)
            val options = markerCapture.captured
            every { marker.position } returns options.position
            every { marker.title } returns options.title
            every { marker.isInfoWindowShown } returns false
            capturedMarkers[marker.title] = marker
            marker
        }

        every { safeBusMap.latLngBounds } returns LatLngBounds(LatLng(47.599274, -122.333282), LatLng(48.599274, -121.333282))

        val directionCapture = slot<Direction>()
        every { iconResource.getIcon(capture(directionCapture)) } answers {
            val bitmap = mockk<BitmapDescriptor>()
            capturedIcons[directionCapture.captured] = bitmap
            bitmap
        }

        subject = MarkerOverlay(safeBusMap, iconResource, 2)
    }

    @Test
    fun adds_stops() {
        subject.addStops(listOf(northStop, southStop))

        verify { safeBusMap.addMarker(markerStopMatcher(northStop)) }
        verify { safeBusMap.addMarker(markerStopMatcher(southStop)) }
    }

    @Test
    fun does_not_add_repeats() {
        subject.addStops(listOf(northStop))
        subject.addStops(listOf(southStop, northStop))

        verify(exactly = 1) { safeBusMap.addMarker(markerStopMatcher(northStop)) }
        verify(exactly = 1) { safeBusMap.addMarker(markerStopMatcher(southStop)) }
    }

    @Test
    fun limits_number_of_stops_added_at_once() {
        subject.addStops(listOf(northStop, southStop, noDirectionStop))

        verify(exactly = 2) { safeBusMap.addMarker(any()) }
    }

    @Test
    fun removes_far_stops_when_limit_is_reached() {
        subject.addStops(listOf(northStop, noDirectionStop))
        subject.addStops(listOf(southStop))

        verify { safeBusMap.addMarker(markerStopMatcher(southStop)) }
        verify { capturedMarkers[noDirectionStop.name]?.remove() }
        verify(exactly = 0) { capturedMarkers[northStop.name]?.remove() }
    }

    @Test
    fun does_not_remove_selected_marker() {
        val subject = MarkerOverlay(safeBusMap, iconResource, 1)

        subject.addStops(listOf(northStop))
        every { capturedMarkers[northStop.name]?.isInfoWindowShown } returns true
        subject.addStops(listOf(southStop))

        verify(exactly = 0) { capturedMarkers[northStop.name]?.remove() }
        verify { safeBusMap.addMarker(markerStopMatcher(southStop)) }
    }

    @Test
    fun adding_duplicate_does_not_count_towards_limit() {
        subject.addStops(listOf(northStop))
        subject.addStops(listOf(northStop, southStop))

        verify(exactly = 0) { capturedMarkers[northStop.name]?.remove() }
    }

    @Test
    fun does_not_remove_markers_that_will_be_displayed() {
        subject.addStops(listOf(northStop, southStop))
        subject.addStops(listOf(northStop, noDirectionStop))

        verify(exactly = 0) { capturedMarkers[northStop.name]?.remove() }
        verify { capturedMarkers[southStop.name]?.remove() }
    }
}