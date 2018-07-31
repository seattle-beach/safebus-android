package io.pivotal.safebus

import android.Manifest
import android.support.test.annotation.UiThreadTest
import android.support.test.rule.ActivityTestRule
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class BusMapActivityInstrumentedTest {
    @get:Rule
    val activityRule = ActivityTestRule(BusMapActivity::class.java)

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION)

    lateinit var activity: BusMapActivity

    @Before
    fun setup() {
        activity = activityRule.activity
    }

    @Test
    @UiThreadTest
    fun location_gets_enabled() {
        assertTrue(activity.map.isMyLocationEnabled)
    }

    @Test
    @UiThreadTest
    fun map_is_moved() {
        assertEquals(47.5989794, activity.map.cameraPosition.target.latitude, Double.MIN_VALUE)
    }
}