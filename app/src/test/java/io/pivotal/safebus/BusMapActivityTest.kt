package io.pivotal.safebus

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BusMapActivityTest {
    @Test
    fun starts_activity() {
        Robolectric.setupActivity(BusMapActivity::class.java)
    }
}