package io.pivotal.safebus.api

data class BusStop(val name: String, val lat: Double, val lon: Double, val direction: Direction = Direction.NONE)

enum class Direction(val direction: String) {
    NORTH("N"),
    SOUTH("S"),
    EAST("E"),
    WEST("W"),
    NORTHEAST("NE"),
    NORTHWEST("NW"),
    SOUTHEAST("SE"),
    SOUTHWEST("SW"),
    NONE("")
}