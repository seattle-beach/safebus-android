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

    lateinit var bitmapDescriptor: BitmapDescriptor
    lateinit var bitmapCreator: BitmapCreator

    lateinit var subject: BusIconResource

    @Before
    fun setup() {
        bitmapDescriptor = mockk()
        bitmapCreator = mockk()
        every { bitmapCreator.createBitmap(any()) } returns bitmapDescriptor

        subject = BusIconResource(bitmapCreator)
    }

    @Test
    fun returns_bitmap_for_east_direction() {
        val actualBitmap = subject.getIcon(Direction.EAST)
        assertEquals(bitmapDescriptor, actualBitmap)

        verify { bitmapCreator.createBitmap(R.drawable.bus_icon_east) }
    }

    @Test
    fun returns_bitmap_for_west_direction() {
        val actualBitmap = subject.getIcon(Direction.WEST)
        assertEquals(bitmapDescriptor, actualBitmap)

        verify { bitmapCreator.createBitmap(R.drawable.bus_icon_west) }
    }

    @Test
    fun returns_bitmap_for_north_direction() {
        val actualBitmap = subject.getIcon(Direction.NORTH)
        assertEquals(bitmapDescriptor, actualBitmap)

        verify { bitmapCreator.createBitmap(R.drawable.bus_icon_north) }
    }

    @Test
    fun returns_bitmap_for_south_direction() {
        val actualBitmap = subject.getIcon(Direction.SOUTH)
        assertEquals(bitmapDescriptor, actualBitmap)

        verify { bitmapCreator.createBitmap(R.drawable.bus_icon_south) }
    }

    @Test
    fun returns_bitmap_for_southeast_direction() {
        val actualBitmap = subject.getIcon(Direction.SOUTHEAST)
        assertEquals(bitmapDescriptor, actualBitmap)

        verify { bitmapCreator.createBitmap(R.drawable.bus_icon_southeast) }
    }

    @Test
    fun returns_bitmap_for_northeast_direction() {
        val actualBitmap = subject.getIcon(Direction.NORTHEAST)
        assertEquals(bitmapDescriptor, actualBitmap)

        verify { bitmapCreator.createBitmap(R.drawable.bus_icon_northeast) }
    }

    @Test
    fun returns_bitmap_for_northwest_direction() {
        val actualBitmap = subject.getIcon(Direction.NORTHWEST)
        assertEquals(bitmapDescriptor, actualBitmap)

        verify { bitmapCreator.createBitmap(R.drawable.bus_icon_northwest) }
    }

    @Test
    fun returns_bitmap_for_southwest_direction() {
        val actualBitmap = subject.getIcon(Direction.SOUTHWEST)
        assertEquals(bitmapDescriptor, actualBitmap)

        verify { bitmapCreator.createBitmap(R.drawable.bus_icon_southwest) }
    }

    @Test
    fun returns_bitmap_for_no_direction() {
        val actualBitmap = subject.getIcon(Direction.NONE)
        assertEquals(bitmapDescriptor, actualBitmap)

        verify { bitmapCreator.createBitmap(R.drawable.bus_icon_no_direction) }
    }

    @Test
    fun caches_bitmaps() {
        subject.getIcon(Direction.EAST)
        subject.getIcon(Direction.EAST)

        verify(exactly = 1) { bitmapCreator.createBitmap(R.drawable.bus_icon_east) }
    }
}