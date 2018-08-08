package io.pivotal.safebus

import android.content.Context

class FavoriteStopsRepository(context: Context) {
    val preferences = context.getSharedPreferences(context.getString(R.string.favorite_stops_preferences), Context.MODE_PRIVATE)
    val favoriteKey = context.getString(R.string.favorite_stops_key)

    fun exists(favoriteStopId: String): Boolean =
            preferences.getStringSet(favoriteKey, emptySet())!!.contains(favoriteStopId)

    fun toggle(favoriteStopId: String): Boolean {
        val favoriteStops = preferences.getStringSet(favoriteKey, mutableSetOf())!!

        if (favoriteStops.contains(favoriteStopId)) {
            favoriteStops.remove(favoriteStopId)
        } else {
            favoriteStops.add(favoriteStopId)
        }

        with(preferences.edit()) {
            putStringSet(favoriteKey, favoriteStops)
            apply()
        }

        return exists(favoriteStopId)
    }
}
