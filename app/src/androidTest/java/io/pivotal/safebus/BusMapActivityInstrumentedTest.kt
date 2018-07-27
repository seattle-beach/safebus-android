package io.pivotal.safebus

import android.Manifest
import android.support.test.annotation.UiThreadTest
import android.support.test.rule.ActivityTestRule
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class BusMapActivityInstrumentedTest {
    @get:Rule
    val activityRule = ActivityTestRule(BusMapActivity::class.java)

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION)

    @UiThreadTest
    @Test
    fun activity_starts() {
        val myLocationEnabled = activityRule.activity.map.isMyLocationEnabled

        assertTrue(myLocationEnabled)
    }
}