package io.pivotal.safebus

import android.Manifest
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.tasks.Tasks
import com.tbruyelle.rxpermissions2.RxPermissions
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import io.pivotal.safebus.api.BusStop
import io.pivotal.safebus.api.Direction
import io.pivotal.safebus.api.SafeBusApi
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.BehaviorSubject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.standalone.inject
import org.koin.test.KoinTest
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController


@RunWith(RobolectricTestRunner::class)
class BusMapActivityTest : KoinTest {
    private val location = Location("")
    private val PIVOTAL_LOCATION = CameraPosition.Builder()
            .target(LatLng(47.5989794, -122.335976))
            .zoom(16.0f)
            .build()

    private val safeBusApi by inject<SafeBusApi>()
    private val locationClient by inject<FusedLocationProviderClient>()
    private val mapEmitter by inject<MapEmitter>()
    private val _ioScheduler by inject<Scheduler>("io")
    private val _uiScheduler by inject<Scheduler>("ui")
    private val rxPermissions by inject<RxPermissions>()

    private val ioScheduler = _ioScheduler as TestScheduler
    private val uiScheduler = _uiScheduler as TestScheduler

    private lateinit var subject: BusMapActivity
    private lateinit var mapIdleStream: BehaviorSubject<LatLngBounds>
    private lateinit var busTappedStream: BehaviorSubject<BusStop>

    @MockK
    lateinit var safeBusMap: SafeBusMap

    private lateinit var subjectController: ActivityController<BusMapActivity>

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mapIdleStream = BehaviorSubject.create()
        busTappedStream = BehaviorSubject.create()

        every { mapEmitter.mapReady() } returns Single.just(safeBusMap)
        every { safeBusMap.cameraIdle() } returns mapIdleStream
        every { safeBusMap.busStopTapped() } returns busTappedStream

        subjectController = Robolectric.buildActivity(BusMapActivity::class.java)
        subject = subjectController.get()

        location.longitude = 12.32
        location.latitude = -82.3
        every { locationClient.lastLocation } returns Tasks.forResult(location)
    }

    @Test
    fun locationGetsEnabled_ifPermissionsAreGranted() {
        every { rxPermissions.request(ACCESS_FINE_LOCATION) } returns Observable.just(true)
        val busStops = listOf(BusStop("S Jackson St & Occidental Ave Walk", 47.599274, -122.333282, Direction.NORTH))
        every {
            safeBusApi.findBusStops(any(), any(), any(), any(), any())
        } returns Observable.just(busStops)

        shadowOf(subject).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        subjectController.setup()
        val position = CameraPosition.Builder()
                .target(LatLng(location.latitude, location.longitude))
                .zoom(16.0f)
                .build()

        mapIdleStream.onNext(LatLngBounds(
                LatLng(location.latitude - 0.015, location.longitude - 0.01),
                LatLng(location.latitude + 0.015, location.longitude + 0.01)
        ))

        ioScheduler.triggerActions()
        uiScheduler.triggerActions()

        verify {
            safeBusApi.findBusStops(
                    location.latitude,
                    location.longitude,
                    range(0.03 - 0.0000001, 0.03 + 0.0000001),
                    range(0.02 - 0.0000001, 0.02 + 0.0000001),
                    50
            )
        }

        verify { safeBusMap.isMyLocationEnabled = true }
        verify { safeBusMap.moveCamera(position) }
        verify { safeBusMap.addStops(busStops) }
    }

    @Test
    fun centersMapWithoutRezooming_whenBusIsTapped() {
        every { rxPermissions.request(ACCESS_FINE_LOCATION) } returns Observable.just(true)
        every { locationClient.lastLocation } returns Tasks.forResult(null)

        subjectController.setup()

        every { safeBusMap.cameraPosition } returns CameraPosition.fromLatLngZoom(LatLng(20.0, 20.0), 12.0f)

        busTappedStream.onNext(BusStop(name = "FOO", lat = 12.2, lon = 12.3, direction = Direction.NONE))

        verify { safeBusMap.animateCamera(CameraPosition.fromLatLngZoom(LatLng(12.2, 12.3), 12.0f)) }
    }

    @Test
    fun locationAllowed_butNoLocation_movesToPivotal() {
        every { rxPermissions.request(ACCESS_FINE_LOCATION) } returns Observable.just(true)
        every { locationClient.lastLocation } returns Tasks.forResult(null)

        subjectController.setup()

        verify { safeBusMap.moveCamera(PIVOTAL_LOCATION) }
    }

    @Test
    fun locationNotAllowed_movesToPivotal() {
        every { rxPermissions.request(ACCESS_FINE_LOCATION) } returns Observable.just(false)
        subjectController.setup()
        subject.onRequestPermissionsResult(42,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                intArrayOf(PackageManager.PERMISSION_DENIED)
        )

        verify(exactly = 0) { safeBusMap.isMyLocationEnabled = true }
        verify(exactly = 0) { locationClient.lastLocation }
        verify { safeBusMap.moveCamera(PIVOTAL_LOCATION) }
    }

    @Test
    fun locationNotInitiallyAllowed_movesToLocationAfterAllowed() {
        every { rxPermissions.request(ACCESS_FINE_LOCATION) } returns Observable.just(true, true)

        subjectController.setup()
        subject.onRequestPermissionsResult(42,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                intArrayOf(PackageManager.PERMISSION_GRANTED)
        )

        every {
            safeBusApi.findBusStops(any(), any(), any(), any())
        } returns Observable.just(ArrayList())

        verify { safeBusMap.isMyLocationEnabled = true }
        verify {
            safeBusMap.moveCamera(CameraPosition.Builder()
                    .target(LatLng(location.latitude, location.longitude))
                    .zoom(16.0f)
                    .build())
        }
    }
}