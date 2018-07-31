package io.pivotal.safebus

import com.google.android.gms.location.FusedLocationProviderClient
import io.mockk.mockk
import io.pivotal.safebus.api.SafeBusApi
import org.koin.dsl.module.Module
import org.koin.standalone.StandAloneContext
import org.robolectric.TestLifecycleApplication
import java.lang.reflect.Method

class TestSafeBusApplication : SafeBusApplication(), TestLifecycleApplication {
    override fun prepareTest(test: Any?) {
    }

    override fun afterTest(method: Method?) {
        StandAloneContext.closeKoin()
    }

    override fun beforeTest(method: Method?) {
        StandAloneContext.startKoin(listOf(testModules))
    }

    private val testModules: Module = org.koin.dsl.module.applicationContext {
        bean { mockk<SafeBusApi>() }
        bean { mockk<FusedLocationProviderClient>() }
        bean { params -> mockk<MapEmitter>() }
    }

    override fun onCreate() {
    }
}