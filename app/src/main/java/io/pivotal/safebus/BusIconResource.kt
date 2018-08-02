package io.pivotal.safebus

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import io.pivotal.safebus.api.Direction

class BitmapCreator(private val context: Context) {
    fun createBitmap(resource: Int): BitmapDescriptor {
        val drawable = context.getDrawable(resource)
        val canvas = Canvas()
        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        canvas.setBitmap(bitmap)
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}

class BusIconResource(private val creator: BitmapCreator) {
    private val cache: MutableMap<Direction, BitmapDescriptor> = HashMap()

    fun getIcon(direction: Direction): BitmapDescriptor {
        return cache.getOrPut(direction, { create(direction) })
    }

    private fun create(direction: Direction): BitmapDescriptor {
        val resource = when (direction) {
            Direction.NORTH -> R.drawable.bus_icon_north
            Direction.SOUTH -> R.drawable.bus_icon_south
            Direction.EAST -> R.drawable.bus_icon_east
            Direction.WEST -> R.drawable.bus_icon_west
            Direction.NORTHWEST -> R.drawable.bus_icon_northwest
            Direction.NORTHEAST -> R.drawable.bus_icon_northeast
            Direction.SOUTHWEST -> R.drawable.bus_icon_southwest
            Direction.SOUTHEAST -> R.drawable.bus_icon_southeast
            Direction.NONE -> R.drawable.bus_icon_no_direction
        }
        return creator.createBitmap(resource)
    }
}