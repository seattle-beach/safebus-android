package io.pivotal.safebus

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Tasks
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import io.pivotal.safebus.api.SafeBusApi
import io.reactivex.Observable
import io.reactivex.Single
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

    val safeBusApi by inject<SafeBusApi>()
    val locationClient by inject<FusedLocationProviderClient>()
    val mapEmitter by inject<MapEmitter>(parameters = { emptyMap() })

    @MockK
    lateinit var safeBusMap: SafeBusMap

    private lateinit var subjectController: ActivityController<BusMapActivity>

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { mapEmitter.mapReady() } returns Single.just(safeBusMap)

        subjectController = Robolectric.buildActivity(BusMapActivity::class.java)
        subject = subjectController.get()

        location.longitude = 12.32
        location.latitude = -122.2
        every { locationClient.lastLocation } returns Tasks.forResult(location)
    }

    @Test
    fun locationGetsEnabled_ifPermissionsAreGranted() {
        shadowOf(subject).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        subjectController.setup()

        every {
            safeBusApi.findBusStops(any(), any(), any(), any())
        } returns Observable.just(ArrayList())

        verify { safeBusMap.isMyLocationEnabled = true }
        verify {
            safeBusMap.moveCamera(CameraPosition.Builder()
                    .target(LatLng(location.latitude, location.longitude))
                    .zoom(15.0f)
                    .build())
        }
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
                    .zoom(15.0f)
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
                    .zoom(15.0f)
                    .build())
        }
    }
}