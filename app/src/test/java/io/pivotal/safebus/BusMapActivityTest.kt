package io.pivotal.safebus

import android.Manifest
import com.google.android.gms.maps.GoogleMap
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf


@RunWith(RobolectricTestRunner::class)
class BusMapActivityTest {
    private lateinit var subject: BusMapActivity

    @MockK
    lateinit var mockMap: GoogleMap

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        subject = Robolectric.setupActivity(BusMapActivity::class.java)
        val shadowActivity = shadowOf(subject)
        shadowActivity.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION);

        subject.onMapReady(mockMap)
    }

    @Test
    fun location_gets_enabled() {
        verify { mockMap.isMyLocationEnabled = true }
    }
}