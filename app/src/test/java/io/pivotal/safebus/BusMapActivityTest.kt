package io.pivotal.safebus

import android.Manifest
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
import org.robolectric.shadows.ShadowActivity


@RunWith(RobolectricTestRunner::class)
class BusMapActivityTest : KoinTest {
    private lateinit var subject: BusMapActivity
    private lateinit var shadowSubject: ShadowActivity
    private var location = Location("")

    val safeBusApi by inject<SafeBusApi>()
    val locationClient by inject<FusedLocationProviderClient>()
    val mapEmitter by inject<MapEmitter>(parameters = { emptyMap() })

    @MockK
    lateinit var safeBusMap: SafeBusMap

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { mapEmitter.mapReady() } returns Single.just(safeBusMap)

        val builder = Robolectric.buildActivity(BusMapActivity::class.java)

        shadowSubject = shadowOf(builder.get())
        shadowSubject.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

        location.longitude = 12.32
        location.latitude = -122.2
        every { locationClient.lastLocation } returns Tasks.forResult(location)

        subject = builder.setup().get()
    }

    @Test
    fun locationGetsEnabled_ifPermissionsAreGranted() {

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
        every {
            safeBusApi.findBusStops(any(), any(), any(), any())
        } returns Observable.just(ArrayList())

        shadowSubject.denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

        verify(exactly = 0) { safeBusMap.isMyLocationEnabled = true }
        verify(exactly = 0) { locationClient.lastLocation }
//        verify { safeBusMap.moveCamera(CameraPosition.Builder()
//                .target(LatLng(47.5989794, -122.335976))
//                .zoom(15.0f)
//                .build())
//        }
    }
}