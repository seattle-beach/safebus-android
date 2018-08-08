package io.pivotal.safebus

import com.google.android.gms.maps.model.*
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.pivotal.safebus.api.BusStop
import io.pivotal.safebus.api.Direction
import io.reactivex.observers.TestObserver
import org.junit.Assert.*
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

    private val northStop = BusStop("1_1", 47.599274, -122.333282, Direction.NORTH, "James St. 1")
    private val southStop = BusStop("1_2", 48.599274, -122.333282, Direction.SOUTH, "James St. 2")
    private val noDirectionStop = BusStop("1_3", 49.599274, -122.333282, Direction.NONE, "James St. 3")

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
            if (options.title != null) {
                every { marker.title } returns options.title
                capturedMarkers[marker.title] = marker
            } else {
                capturedMarkers["NO_TITLE"] = marker
            }
            every { marker.isInfoWindowShown } returns false
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
    fun addsStops() {
        subject.addStops(listOf(northStop, southStop))

        verify { safeBusMap.addMarker(markerStopMatcher(northStop)) }
        verify { safeBusMap.addMarker(markerStopMatcher(southStop)) }
    }

    @Test
    fun doesNotAddRepeats() {
        subject.addStops(listOf(northStop))
        subject.addStops(listOf(southStop, northStop))

        verify(exactly = 1) { safeBusMap.addMarker(markerStopMatcher(northStop)) }
        verify(exactly = 1) { safeBusMap.addMarker(markerStopMatcher(southStop)) }
    }

    @Test
    fun limitsNumberOfStopsAddedAtOnce() {
        subject.addStops(listOf(northStop, southStop, noDirectionStop))

        verify(exactly = 2) { safeBusMap.addMarker(any()) }
    }

    @Test
    fun removesFarStopsWhenLimitIsReached() {
        subject.addStops(listOf(northStop, noDirectionStop))
        subject.addStops(listOf(southStop))

        verify { safeBusMap.addMarker(markerStopMatcher(southStop)) }
        verify { capturedMarkers[noDirectionStop.name]?.remove() }
        verify(exactly = 0) { capturedMarkers[northStop.name]?.remove() }
    }

    @Test
    fun doesNotRemoveSelectedMarker() {
        val subject = MarkerOverlay(safeBusMap, iconResource, 1)

        subject.addStops(listOf(northStop))
        every { capturedMarkers[northStop.name]?.isInfoWindowShown } returns true
        subject.addStops(listOf(southStop))

        verify(exactly = 0) { capturedMarkers[northStop.name]?.remove() }
        verify { safeBusMap.addMarker(markerStopMatcher(southStop)) }
    }

    @Test
    fun addingDuplicateDOesNotCountTowardsLimit() {
        subject.addStops(listOf(northStop))
        subject.addStops(listOf(northStop, southStop))

        verify(exactly = 0) { capturedMarkers[northStop.name]?.remove() }
    }

    @Test
    fun doesNotRemoveMarkersThatWillBeDisplayed() {
        subject.addStops(listOf(northStop, southStop))
        subject.addStops(listOf(northStop, noDirectionStop))

        verify(exactly = 0) { capturedMarkers[northStop.name]?.remove() }
        verify { capturedMarkers[southStop.name]?.remove() }
    }

    @Test
    fun alertsOnBusStopTapped() {
        val observer = TestObserver<BusStop>()
        subject.busStopTapped().subscribe(observer)
        subject.addStops(listOf(northStop, southStop))
        subject.onMarkerClicked.onNext(capturedMarkers[northStop.name]!!)
        subject.onMarkerClicked.onNext(capturedMarkers[southStop.name]!!)

        observer.assertNoErrors()
        observer.assertValues(northStop, southStop)
    }

    @Test
    fun addsADefaultMarkerWhereTapped() {
        val observer = TestObserver<BusStop>()
        subject.busStopTapped().subscribe(observer)
        subject.addStops(listOf(northStop))
        subject.onMarkerClicked.onNext(capturedMarkers[northStop.name]!!)
        val noTitleMarker = capturedMarkers["NO_TITLE"]
        assertNotNull(noTitleMarker)
        assertEquals(noTitleMarker?.position, LatLng(northStop.lat, northStop.lon))
    }

    @Test
    fun noAlertIfNonStopMarkerIsTapped() {
        val observer = TestObserver<BusStop>()
        subject.busStopTapped().subscribe(observer)
        subject.addStops(listOf(northStop))
        subject.onMarkerClicked.onNext(capturedMarkers[northStop.name]!!)
        subject.onMarkerClicked.onNext(capturedMarkers["NO_TITLE"]!!)
        observer.assertValue(northStop)
    }

    @Test
    fun switchesMarkerOnSecondTap() {
        val observer = TestObserver<BusStop>()
        subject.busStopTapped().subscribe(observer)
        subject.addStops(listOf(northStop, southStop))

        subject.onMarkerClicked.onNext(capturedMarkers[northStop.name]!!)
        val firstNoTitleMarker = capturedMarkers["NO_TITLE"]!!

        subject.onMarkerClicked.onNext(capturedMarkers[southStop.name]!!)
        val secondNoTitleMarker = capturedMarkers["NO_TITLE"]!!

        assertNotEquals(firstNoTitleMarker, secondNoTitleMarker)
        verify { firstNoTitleMarker.remove() }
        verify(exactly = 0) { secondNoTitleMarker.remove() }
    }

    @Test
    fun noReactionForRemovedStop() {
        val observer = TestObserver<BusStop>()
        subject.busStopTapped().subscribe(observer)
        subject.addStops(listOf(northStop, southStop))
        subject.addStops(listOf(northStop, noDirectionStop))

        subject.onMarkerClicked.onNext(capturedMarkers[southStop.name]!!)
        observer.assertNoValues()
    }
}