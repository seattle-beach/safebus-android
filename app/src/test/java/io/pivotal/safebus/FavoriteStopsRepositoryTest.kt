package io.pivotal.safebus

import android.content.Context
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class FavoriteStopsRepositoryTest {

    private val context = RuntimeEnvironment.application

    @Test
    fun savesFavoriteStopId() {
        val subject = FavoriteStopsRepository(context)
        val sharedPreferences = context.getSharedPreferences(
                context.getString(R.string.favorite_stops_preferences),
                Context.MODE_PRIVATE
        )

        val added = subject.toggle("1_1")

        assertTrue(added)
        assertTrue(subject.exists("1_1"))

        assertTrue(sharedPreferences.getStringSet(context.getString(R.string.favorite_stops_key), setOf())!!.contains("1_1"))
    }

    @Test
    fun savesMultipletimes() {
        val subject = FavoriteStopsRepository(context)
        val sharedPreferences = context.getSharedPreferences(
                context.getString(R.string.favorite_stops_preferences),
                Context.MODE_PRIVATE
        )

        subject.toggle("1_1")
        subject.toggle("1_2")

        assertTrue(subject.exists("1_1"))
        assertTrue(subject.exists("1_2"))

        val favoriteStops = sharedPreferences.getStringSet(context.getString(R.string.favorite_stops_key), setOf())!!
        assertTrue(favoriteStops.contains("1_1"))
        assertTrue(favoriteStops.contains("1_2"))
    }

    @Test
    fun removesStopsFromFavorites() {
        val subject = FavoriteStopsRepository(context)
        val sharedPreferences = context.getSharedPreferences(
                context.getString(R.string.favorite_stops_preferences),
                Context.MODE_PRIVATE
        )

        subject.toggle("1_1")
        val isFavorite = subject.toggle("1_1")

        assertFalse(isFavorite)
        assertFalse(subject.exists("1_1"))

        assertFalse(sharedPreferences.getStringSet(context.getString(R.string.favorite_stops_key), setOf())!!.contains("1_1"))
    }
}