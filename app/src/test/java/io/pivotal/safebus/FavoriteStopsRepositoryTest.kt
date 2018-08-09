package io.pivotal.safebus

import android.content.Context
import android.content.SharedPreferences
import io.reactivex.observers.TestObserver
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class FavoriteStopsRepositoryTest {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var subject: FavoriteStopsRepository
    private lateinit var observer: TestObserver<Pair<String, Boolean>>
    private lateinit var keyString: String

    @Before
    fun setup() {
        val context = RuntimeEnvironment.application
        subject = FavoriteStopsRepository(context)
        sharedPreferences = context.getSharedPreferences(
                context.getString(R.string.favorite_stops_preferences),
                Context.MODE_PRIVATE
        )
        keyString = context.getString(R.string.favorite_stops_key)
        observer = TestObserver()
        subject.onToggle().subscribe(observer)
    }

    @Test
    fun savesFavoriteStopId() {
        val added = subject.toggle("1_1")

        assertTrue(added)
        assertTrue(subject.isFavorite("1_1"))

        assertTrue(favoriteStops().contains("1_1"))

        observer.assertValue(Pair("1_1", true))
    }

    @Test
    fun savesMultipletimes() {
        subject.toggle("1_1")
        subject.toggle("1_2")

        assertTrue(subject.isFavorite("1_1"))
        assertTrue(subject.isFavorite("1_2"))

        val favoriteStops = favoriteStops()
        assertTrue(favoriteStops.contains("1_1"))
        assertTrue(favoriteStops.contains("1_2"))

        observer.assertValues(Pair("1_1", true), Pair("1_2", true))
    }

    @Test
    fun removesStopsFromFavorites() {
        subject.toggle("1_1")
        val isFavorite = subject.toggle("1_1")

        assertFalse(isFavorite)
        assertFalse(subject.isFavorite("1_1"))

        assertFalse(favoriteStops().contains("1_1"))

        observer.assertValues(Pair("1_1", true), Pair("1_1", false))
    }

    private fun favoriteStops(): MutableSet<String> = sharedPreferences.getStringSet(keyString, setOf())!!
}