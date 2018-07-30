package io.pivotal.safebus

import android.Manifest
import com.google.android.gms.maps.GoogleMap
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import io.pivotal.safebus.api.SafeBusService
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowActivity


@RunWith(RobolectricTestRunner::class)
class BusMapActivityTest {
    private lateinit var subject: BusMapActivity
    private lateinit var shadowSubject: ShadowActivity

    @MockK
    lateinit var mockMap: GoogleMap

    //@MockK
    //lateinit var mockBusService: SafeBusService

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        subject = Robolectric.setupActivity(BusMapActivity::class.java)
        shadowSubject = shadowOf(subject)

    }

    @Test
    fun locationGetsEnabled_ifPermissionsAreGranted() {
        shadowSubject.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        subject.onMapReady(mockMap)
        verify { mockMap.isMyLocationEnabled = true }
    }

    @Test
    fun defaultsToPivotal_ifPermissionsAreDenied() {
        shadowSubject.denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        subject.onMapReady(mockMap)

        verify(exactly = 0) { mockMap.isMyLocationEnabled = true }
    }

    @Test
    fun serviceCalledWithCurrentLocation_ifPermissionsAreGranted() {
        shadowSubject.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

        //every { mockBusService.findBusStops() } returns ArrayList()

        subject.onMapReady(mockMap)

        //verify { mockBusService.findBusStops(...) }
    }
}