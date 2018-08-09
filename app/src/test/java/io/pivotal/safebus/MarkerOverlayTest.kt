package io.pivotal.safebus

import com.google.android.gms.maps.model.*
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.pivotal.safebus.api.BusStop
import io.pivotal.safebus.api.Direction
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MarkerOverlayTest {

    @MockK
    private lateinit var safeBusMap: SafeBusMap
    @MockK
    private lateinit var iconResource: BusIconResource
    @MockK
    private lateinit var favoriteStopsRepository: FavoriteStopsRepository

    private lateinit var capturedMarkers: MutableMap<String, Marker>
    private lateinit var capturedIcons: MutableMap<Pair<Direction, Boolean>, BitmapDescriptor>
    private lateinit var observer: TestObserver<SafeBusMarker>
    private lateinit var favoriteStopsStream: PublishSubject<Pair<String, Boolean>>
    private lateinit var subject: MarkerOverlay

    private val northStop = BusStop("1_1", 47.599274, -122.333282, Direction.NORTH, "James St. 1")
    private val southStop = BusStop("1_2", 48.599274, -122.333282, Direction.SOUTH, "James St. 2")
    private val noDirectionStop = BusStop("1_3", 49.599274, -122.333282, Direction.NONE, "James St. 3")

    private fun MockKVerificationScope.markerStopMatcher(firstStop: BusStop, isFavorite: Boolean): MarkerOptions =
            match { m -> m.isMarkerForStop(firstStop, isFavorite) }

    private fun MarkerOptions.isMarkerForStop(stop: BusStop, isFavorite: Boolean): Boolean =
            position == LatLng(stop.lat, stop.lon)
                    && title == stop.name
                    && icon == capturedIcons[Pair(stop.direction, isFavorite)]
                    && anchorU == 0.5f
                    && anchorV == 0.5f
                    && icon != null

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        capturedMarkers = HashMap()
        capturedIcons = HashMap()
        observer = TestObserver()

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
            marker
        }

        every { safeBusMap.latLngBounds } returns LatLngBounds(LatLng(47.599274, -122.333282), LatLng(48.599274, -121.333282))

        val direction = slot<Direction>()
        val isFavorite = slot<Boolean>()
        every { iconResource.getIcon(capture(direction), capture(isFavorite)) } answers {
            val bitmap = mockk<BitmapDescriptor>()
            capturedIcons[Pair(direction.captured, isFavorite.captured)] = bitmap
            bitmap
        }

        favoriteStopsStream = PublishSubject.create()
        every { favoriteStopsRepository.isFavorite(any()) } returns false
        every { favoriteStopsRepository.onToggle() } returns favoriteStopsStream

        subject = MarkerOverlay(safeBusMap, iconResource, favoriteStopsRepository, 2)
        subject.busStopTapped().subscribe(observer)
    }

    @Test
    fun addsStops() {
        subject.addStops(listOf(northStop, southStop))

        verify { safeBusMap.addMarker(markerStopMatcher(northStop, false)) }
        verify { safeBusMap.addMarker(markerStopMatcher(southStop, false)) }
    }

    @Test
    fun doesNotAddRepeats() {
        subject.addStops(listOf(northStop))
        subject.addStops(listOf(southStop, northStop))

        verify(exactly = 1) { safeBusMap.addMarker(markerStopMatcher(northStop, false)) }
        verify(exactly = 1) { safeBusMap.addMarker(markerStopMatcher(southStop, false)) }
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

        verify { safeBusMap.addMarker(markerStopMatcher(southStop, false)) }
        verify { capturedMarkers[noDirectionStop.name]?.remove() }
        verify(exactly = 0) { capturedMarkers[northStop.name]?.remove() }
    }

    @Test
    fun doesNotRemoveSelectedMarker() {
        subject.addStops(listOf(northStop, southStop))
        subject.onMarkerClicked.onNext(markerForStop(northStop))

        subject.addStops(listOf(southStop, noDirectionStop))

        verify(exactly = 0) { capturedMarkers[northStop.name]?.remove() }
        verify { safeBusMap.addMarker(markerStopMatcher(southStop, false)) }
    }

    @Test
    fun removesPreviouslySelectedMarker() {
        subject.addStops(listOf(northStop, southStop))
        subject.onMarkerClicked.onNext(markerForStop(northStop))
        subject.onMarkerClicked.onNext(markerForStop(southStop))

        subject.addStops(listOf(southStop, noDirectionStop))

        verify { capturedMarkers[northStop.name]?.remove() }
        verify { safeBusMap.addMarker(markerStopMatcher(southStop, false)) }
    }

    @Test
    fun addingDuplicateDoesNotCountTowardsLimit() {
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
        subject.addStops(listOf(northStop, southStop))
        subject.onMarkerClicked.onNext(markerForStop(northStop))
        subject.onMarkerClicked.onNext(markerForStop(southStop))

        observer.assertNoErrors()
        observer.assertValueAt(0) { safeMarker -> safeMarker.id == northStop.id }
        observer.assertValueAt(1) { safeMarker -> safeMarker.id == southStop.id }
        observer.assertValueCount(2)
    }

    @Test
    fun addsADefaultMarkerWhereTapped() {
        subject.addStops(listOf(northStop))
        subject.onMarkerClicked.onNext(markerForStop(northStop))
        val noTitleMarker = capturedMarkers["NO_TITLE"]
        assertNotNull(noTitleMarker)
        assertEquals(noTitleMarker?.position, LatLng(northStop.lat, northStop.lon))
    }

    @Test
    fun noAlertIfNonStopMarkerIsTapped() {
        subject.addStops(listOf(northStop))
        subject.onMarkerClicked.onNext(markerForStop(northStop))
        subject.onMarkerClicked.onNext(noTitleMarker())
        observer.assertValue { safeMarker -> safeMarker.id == northStop.id }

    }

    @Test
    fun switchesMarkerOnSecondTap() {
        subject.addStops(listOf(northStop, southStop))

        subject.onMarkerClicked.onNext(markerForStop(northStop))
        val firstNoTitleMarker = noTitleMarker()

        subject.onMarkerClicked.onNext(markerForStop(southStop))
        val secondNoTitleMarker = noTitleMarker()

        assertNotEquals(firstNoTitleMarker, secondNoTitleMarker)
        verify { firstNoTitleMarker.remove() }
        verify(exactly = 0) { secondNoTitleMarker.remove() }
    }

    @Test
    fun noReactionForRemovedStop() {
        subject.addStops(listOf(northStop, southStop))
        subject.addStops(listOf(northStop, noDirectionStop))

        subject.onMarkerClicked.onNext(markerForStop(southStop))
        observer.assertNoValues()
    }

    @Test
    fun setsMarkerAsFavorite_whenRepositoryUpdates() {
        subject.addStops(listOf(northStop, southStop))
        val northGoogleMarker = markerForStop(northStop)
        val southGoogleMarker = markerForStop(southStop)

        // mark north as favorite and assert
        favoriteStopsStream.onNext(Pair(northStop.id, true))
        subject.onMarkerClicked.onNext(northGoogleMarker)
        observer.assertValueAt(0) { it.isFavorite }
        verify { northGoogleMarker.setIcon(capturedIcons[Pair(Direction.NORTH, true)]) }

        // assert south is not favorite
        subject.onMarkerClicked.onNext(southGoogleMarker)
        observer.assertValueAt(1) { !it.isFavorite }
        verify(exactly = 0) { southGoogleMarker.setIcon(any()) }

        // toggle north back as not favorite and assert
        favoriteStopsStream.onNext(Pair(northStop.id, false))
        subject.onMarkerClicked.onNext(northGoogleMarker)
        observer.assertValueAt(2) { !it.isFavorite }
        verify { northGoogleMarker.setIcon(capturedIcons[Pair(Direction.NORTH, false)]) }
    }

    @Test
    fun respectsInitialIsFavorite() {
        every { favoriteStopsRepository.isFavorite(northStop.id) } returns false
        every { favoriteStopsRepository.isFavorite(southStop.id) } returns true

        subject.addStops(listOf(northStop, southStop))

        verify { safeBusMap.addMarker(markerStopMatcher(northStop, false)) }
        verify { safeBusMap.addMarker(markerStopMatcher(southStop, true)) }


        // north is not favorite
        subject.onMarkerClicked.onNext(markerForStop(northStop))
        observer.assertValueAt(0) { !it.isFavorite }

        // south is favorite
        subject.onMarkerClicked.onNext(markerForStop(southStop))
        observer.assertValueAt(1) { it.isFavorite }
    }

    private fun markerForStop(stop: BusStop) = capturedMarkers[stop.name]!!
    private fun noTitleMarker() = capturedMarkers["NO_TITLE"]!!
}