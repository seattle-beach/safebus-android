package io.pivotal.safebus

import android.content.Context
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class FavoriteStopsRepository(context: Context) {
    val preferences = context.getSharedPreferences(context.getString(R.string.favorite_stops_preferences), Context.MODE_PRIVATE)
    val favoriteKey = context.getString(R.string.favorite_stops_key)
    private val _onToggle = PublishSubject.create<Pair<String, Boolean>>()

    fun isFavorite(favoriteStopId: String): Boolean =
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

        val isFavorite = isFavorite(favoriteStopId)

        _onToggle.onNext(Pair(favoriteStopId, isFavorite))
        return isFavorite
    }

    fun onToggle(): Observable<Pair<String, Boolean>> = _onToggle
}
