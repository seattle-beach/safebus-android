package io.pivotal.safebus

import android.content.Context
import android.graphics.*
import android.support.annotation.DrawableRes
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import io.pivotal.safebus.api.Direction


class BitmapCreator(private val context: Context) {
    fun createBitmap(@DrawableRes resource: Int, isFavorite: Boolean): BitmapDescriptor {
        val drawable = context.getDrawable(resource)!!
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)

        val background = Paint()
        val color = if (isFavorite) {
            Color.rgb(239,83,80)
        } else {
            Color.rgb(135, 206, 235)
        }
        background.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)

        val canvas = Canvas(bitmap)
        canvas.drawRoundRect(
                0.0f,
                0.0f,
                drawable.intrinsicWidth.toFloat(),
                drawable.intrinsicHeight.toFloat(),
                drawable.intrinsicHeight.toFloat() / 6,
                drawable.intrinsicWidth.toFloat() / 6,
                background
        )
        drawable.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}

class BusIconResource(private val creator: BitmapCreator) {
    private val cache: MutableMap<Pair<Direction, Boolean>, BitmapDescriptor> = HashMap()

    fun getIcon(direction: Direction, isFavorite: Boolean): BitmapDescriptor {
        return cache.getOrPut(Pair(direction, isFavorite)) { create(direction, isFavorite) }
    }

    private fun create(direction: Direction, isFavorite: Boolean): BitmapDescriptor {
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

        return creator.createBitmap(resource, isFavorite)
    }
}