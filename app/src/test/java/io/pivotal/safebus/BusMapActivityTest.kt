package io.pivotal.safebus

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.tasks.Tasks
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import io.pivotal.safebus.api.BusStop
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
    private lateinit var subject: BusMapActivity
    private var location = Location("")
    private lateinit var mapIdleStream: BehaviorSubject<SafeBusMap>

    private val safeBusApi by inject<SafeBusApi>()
    private val locationClient by inject<FusedLocationProviderClient>()
    private val mapEmitter by inject<MapEmitter>(parameters = { emptyMap() })
    private val ioScheduler by inject<Scheduler>("io")
    private val uiScheduler by inject<Scheduler>("ui")

    @MockK
    lateinit var safeBusMap: SafeBusMap

    private lateinit var subjectController: ActivityController<BusMapActivity>

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mapIdleStream = BehaviorSubject.create()

        every { mapEmitter.mapReady() } returns Single.just(safeBusMap)
        every { mapEmitter.cameraIdle() } returns mapIdleStream
        every { safeBusMap.markerOverlay } returns mockk(relaxUnitFun = true)

        subjectController = Robolectric.buildActivity(BusMapActivity::class.java)
        subject = subjectController.get()

        location.longitude = 12.32
        location.latitude = -82.3
        every { locationClient.lastLocation } returns Tasks.forResult(location)
    }

    @Test
    fun locationGetsEnabled_ifPermissionsAreGranted() {
        val busStops = listOf(BusStop("S Jackson St & Occidental Ave Walk", 47.599274, -122.333282))
        every {
            safeBusApi.findBusStops(any(), any(), any(), any(), any())
        } returns Observable.just(busStops)

        every { safeBusMap.latLngBounds } returns LatLngBounds(
                LatLng(location.latitude - 0.015, location.longitude - 0.01),
                LatLng(location.latitude + 0.015, location.longitude + 0.01)
        )

        shadowOf(subject).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        subjectController.setup()
        val position = CameraPosition.Builder()
                .target(LatLng(location.latitude, location.longitude))
                .zoom(16.0f)
                .build()

        mapIdleStream.onNext(safeBusMap)

        (ioScheduler as TestScheduler).triggerActions()
        (uiScheduler as TestScheduler).triggerActions()

        verify {
            safeBusApi.findBusStops(
                    location.latitude,
                    location.longitude,
                    range(0.03 - 0.0000001, 0.03 + 0.0000001),
                    range(0.02 - 0.0000001, 0.02 + 0.0000001),
                    100
            )
        }

        verify { safeBusMap.isMyLocationEnabled = true }
        verify { safeBusMap.moveCamera(position) }
        val overlay = safeBusMap.markerOverlay
        verify { overlay.addStops(busStops) }
    }

    @Test
    fun locationNotEnabled_ifPermissionAreDenied() {
        shadowOf(subject).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        subjectController.setup()
        subject.onRequestPermissionsResult(subject.LOCATION_PERMISSION_CODE,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                intArrayOf(PackageManager.PERMISSION_DENIED)
        )

        every {
            safeBusApi.findBusStops(any(), any(), any(), any())
        } returns Observable.just(ArrayList())


        verify(exactly = 0) { safeBusMap.isMyLocationEnabled = true }
        verify(exactly = 0) { locationClient.lastLocation }
        verify {
            safeBusMap.moveCamera(CameraPosition.Builder()
                    .target(LatLng(47.5989794, -122.335976))
                    .zoom(16.0f)
                    .build())
        }
    }

    @Test
    fun locationGetsEnabled_ifUserAllowsPermissions() {
        shadowOf(subject).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        subjectController.setup()
        subject.onRequestPermissionsResult(subject.LOCATION_PERMISSION_CODE,
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