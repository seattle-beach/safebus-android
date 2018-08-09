package io.pivotal.safebus

import com.google.android.gms.maps.model.BitmapDescriptor
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pivotal.safebus.api.Direction
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class BusIconResourceTest {
    private lateinit var bitmapDescriptor: BitmapDescriptor
    private lateinit var bitmapCreator: BitmapCreator
    private lateinit var subject: BusIconResource

    @Before
    fun setup() {
        bitmapDescriptor = mockk()
        bitmapCreator = mockk()
        every { bitmapCreator.createBitmap(any(), any()) } returns bitmapDescriptor

        subject = BusIconResource(bitmapCreator)
    }

    @Test
    fun returnsBitmapForEastDirection() {
        val actualBitmap = subject.getIcon(Direction.EAST, false)
        assertEquals(bitmapDescriptor, actualBitmap)

        verify { bitmapCreator.createBitmap(R.drawable.bus_icon_east, false) }
    }

    @Test
    fun returnsBitmapForWestDirection() {
        val actualBitmap = subject.getIcon(Direction.WEST, false)
        assertEquals(bitmapDescriptor, actualBitmap)

        verify { bitmapCreator.createBitmap(R.drawable.bus_icon_west, false) }
    }

    @Test
    fun returnsBitmapForNorthDirection() {
        val actualBitmap = subject.getIcon(Direction.NORTH, false)
        assertEquals(bitmapDescriptor, actualBitmap)

        verify { bitmapCreator.createBitmap(R.drawable.bus_icon_north, false) }
    }

    @Test
    fun returnsBitmapForSouthDirection() {
        val actualBitmap = subject.getIcon(Direction.SOUTH, false)
        assertEquals(bitmapDescriptor, actualBitmap)

        verify { bitmapCreator.createBitmap(R.drawable.bus_icon_south, false) }
    }

    @Test
    fun returnsBitmapForSouthEastDirection() {
        val actualBitmap = subject.getIcon(Direction.SOUTHEAST, false)
        assertEquals(bitmapDescriptor, actualBitmap)

        verify { bitmapCreator.createBitmap(R.drawable.bus_icon_southeast, false) }
    }

    @Test
    fun returnsBitmapForNorthEastDirection() {
        val actualBitmap = subject.getIcon(Direction.NORTHEAST, false)
        assertEquals(bitmapDescriptor, actualBitmap)

        verify { bitmapCreator.createBitmap(R.drawable.bus_icon_northeast, false) }
    }

    @Test
    fun returnsBitmapForNorthWestDirection() {
        val actualBitmap = subject.getIcon(Direction.NORTHWEST, false)
        assertEquals(bitmapDescriptor, actualBitmap)

        verify { bitmapCreator.createBitmap(R.drawable.bus_icon_northwest, false) }
    }

    @Test
    fun returnsBitmapForSouthWestDirection() {
        val actualBitmap = subject.getIcon(Direction.SOUTHWEST, false)
        assertEquals(bitmapDescriptor, actualBitmap)

        verify { bitmapCreator.createBitmap(R.drawable.bus_icon_southwest, false) }
    }

    @Test
    fun returnsBitmapForNoDirection() {
        val actualBitmap = subject.getIcon(Direction.NONE, false)
        assertEquals(bitmapDescriptor, actualBitmap)

        verify { bitmapCreator.createBitmap(R.drawable.bus_icon_no_direction, false) }
    }

    @Test
    fun respectsIsFavorite() {
        val actualBitmap = subject.getIcon(Direction.NONE, true)
        assertEquals(bitmapDescriptor, actualBitmap)

        verify { bitmapCreator.createBitmap(R.drawable.bus_icon_no_direction, true) }
    }

    @Test
    fun caches_bitmaps() {
        subject.getIcon(Direction.EAST, false)
        verify(exactly = 1) { bitmapCreator.createBitmap(any(), any()) }

        subject.getIcon(Direction.EAST, false)
        verify(exactly = 1) { bitmapCreator.createBitmap(any(), any()) }

        subject.getIcon(Direction.EAST, true)
        verify(exactly = 2) { bitmapCreator.createBitmap(any(), any()) }

        subject.getIcon(Direction.EAST, true)
        verify(exactly = 2) { bitmapCreator.createBitmap(any(), any()) }
    }
}