package io.pivotal.safebus

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.pivotal.safebus.api.BusStop
import org.junit.Before
import org.junit.Test

class MarkerOverlayTest {

    @MockK
    private lateinit var safeBusMap: SafeBusMap

    private lateinit var capturedMarkers: MutableMap<String, Marker>

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        capturedMarkers = HashMap()

        val slot = slot<BusStop>()
        every { safeBusMap.addMarker(capture(slot)) } answers {
            val marker = mockk<Marker>(relaxUnitFun = true)
            val stop = slot.captured
            every { marker.position } returns LatLng(stop.lat, stop.lon)
            every { marker.title } returns stop.name
            capturedMarkers[marker.title] = marker
            marker
        }
    }

    @Test
    fun adds_stops() {
        val subject = MarkerOverlay(safeBusMap)

        val firstStop = BusStop("James St.", 47.599274, -122.333282)
        val secondStop = BusStop("Madison St.", 45.599274, -121.333282)
        subject.addStops(listOf(firstStop, secondStop))

        verify { safeBusMap.addMarker(firstStop) }
        verify { safeBusMap.addMarker(secondStop) }
    }

    @Test
    fun does_not_add_repeats() {
        val subject = MarkerOverlay(safeBusMap)

        val firstStop = BusStop("James St.", 47.599274, -122.333282)
        subject.addStops(listOf(firstStop))
        val secondStop = BusStop("Madison St.", 45.599274, -121.333282)
        subject.addStops(listOf(secondStop, firstStop))

        verify(exactly = 1) { safeBusMap.addMarker(firstStop) }
        verify(exactly = 1) { safeBusMap.addMarker(secondStop) }
    }

    @Test
    fun limits_number_of_stops_added_at_once() {
        val subject = MarkerOverlay(safeBusMap, 3)

        subject.addStops(listOf(
                BusStop("James St. 1", 47.599274, -122.333282),
                BusStop("James St. 2", 48.599274, -122.333282),
                BusStop("James St. 3", 49.599274, -122.333282),
                BusStop("James St. 4", 50.599274, -122.333282)
        ))

        verify(exactly = 3) { safeBusMap.addMarker(any()) }
    }

    @Test
    fun limits_number_of_stops_added_over_many_times() {
        val subject = MarkerOverlay(safeBusMap, 2)

        val firstBusStop = BusStop("James St. 1", 48.599274, -122.333282)
        val secondBusStop = BusStop("James St. 2", 50.599274, -122.333282)
        val thirdBusStop = BusStop("James St. 3", 47.599274, -122.333282)
        val fourthBusStop = BusStop("James St. 4", 49.599274, -122.333282)

        subject.addStops(listOf(firstBusStop, secondBusStop))
        subject.addStops(listOf(thirdBusStop, fourthBusStop))

        verify { capturedMarkers[firstBusStop.name]?.remove() }
        verify { capturedMarkers[secondBusStop.name]?.remove() }
        verify(exactly = 1) { safeBusMap.addMarker(thirdBusStop) }
        verify(exactly = 1) { safeBusMap.addMarker(fourthBusStop) }
    }
}