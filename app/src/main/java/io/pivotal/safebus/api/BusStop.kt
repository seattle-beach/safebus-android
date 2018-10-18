package io.pivotal.safebus.api

import com.google.gson.annotations.SerializedName

data class BusStop(val id: String, val lat: Double, val lon: Double, val direction: Direction = Direction.NONE, val name: String)

enum class Direction {
    @SerializedName("N")
    NORTH,
    @SerializedName("S")
    SOUTH,
    @SerializedName("E")
    EAST,
    @SerializedName("W")
    WEST,
    @SerializedName("NE")
    NORTHEAST,
    @SerializedName("NW")
    NORTHWEST,
    @SerializedName("SE")
    SOUTHEAST,
    @SerializedName("SW")
    SOUTHWEST,
    @SerializedName("")
    NONE
}