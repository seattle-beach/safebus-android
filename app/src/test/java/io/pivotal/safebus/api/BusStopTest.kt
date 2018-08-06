package io.pivotal.safebus.api

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Test

inline fun <reified T> Gson.fromJson(json: String) = this.fromJson<T>(json, object : TypeToken<T>() {}.type)

class BusStopTest {
    @Test
    fun deserializess() {
        val stops = Gson().fromJson<List<BusStop>>(data)
        assertEquals(stops.size, 9)
        assertEquals(stops[0].direction, Direction.NORTH)
        assertEquals(stops[1].direction, Direction.SOUTH)
        assertEquals(stops[2].direction, Direction.EAST)
        assertEquals(stops[3].direction, Direction.WEST)
        assertEquals(stops[4].direction, Direction.SOUTHEAST)
        assertEquals(stops[5].direction, Direction.SOUTHWEST)
        assertEquals(stops[6].direction, Direction.NORTHEAST)
        assertEquals(stops[7].direction, Direction.NORTHWEST)
        assertEquals(stops[8].direction, Direction.NONE)

    }
}

var data =
        "[\n" +
                "  {\n" +
                "    \"id\": \"1_1551\",\n" +
                "    \"direction\": \"N\",\n" +
                "    \"lat\": 47.599274,\n" +
                "    \"lon\": -122.333282,\n" +
                "    \"name\": \"S Jackson St & Occidental Ave Walk\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": \"1_1889\",\n" +
                "    \"direction\": \"S\",\n" +
                "    \"lat\": 47.603561,\n" +
                "    \"lon\": -122.339211,\n" +
                "    \"name\": \"Vashon Island Water Taxi & Colman Dock\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": \"1_360\",\n" +
                "    \"direction\": \"E\",\n" +
                "    \"lat\": 47.60302,\n" +
                "    \"lon\": -122.333282,\n" +
                "    \"name\": \"2nd Ave & Cherry St\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": \"1_361\",\n" +
                "    \"direction\": \"W\",\n" +
                "    \"lat\": 47.602478,\n" +
                "    \"lon\": -122.332809,\n" +
                "    \"name\": \"2nd Ave & James St\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": \"1_375\",\n" +
                "    \"direction\": \"SE\",\n" +
                "    \"lat\": 47.601444,\n" +
                "    \"lon\": -122.331848,\n" +
                "    \"name\": \"2nd Ave Ext S & Yesler Way\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": \"1_490\",\n" +
                "    \"direction\": \"SW\",\n" +
                "    \"lat\": 47.603794,\n" +
                "    \"lon\": -122.332466,\n" +
                "    \"name\": \"3rd Ave & Columbia St\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": \"1_500\",\n" +
                "    \"direction\": \"NE\",\n" +
                "    \"lat\": 47.602234,\n" +
                "    \"lon\": -122.331047,\n" +
                "    \"name\": \"3rd Ave & James St\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": \"1_501\",\n" +
                "    \"direction\": \"NW\",\n" +
                "    \"lat\": 47.602139,\n" +
                "    \"lon\": -122.331055,\n" +
                "    \"name\": \"Pioneer Sq Station - Bay C\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": \"1_502\",\n" +
                "    \"direction\": \"\",\n" +
                "    \"lat\": 47.602608,\n" +
                "    \"lon\": -122.331497,\n" +
                "    \"name\": \"Pioneer Sq Station - Bay D\"\n" +
                "  }\n" +
                "]"