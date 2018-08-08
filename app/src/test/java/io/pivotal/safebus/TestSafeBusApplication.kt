package io.pivotal.safebus

import com.google.android.gms.location.FusedLocationProviderClient
import com.tbruyelle.rxpermissions2.RxPermissions
import io.mockk.mockk
import io.pivotal.safebus.api.SafeBusApi
import io.reactivex.Scheduler
import io.reactivex.schedulers.TestScheduler
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
        bean { mockk<MapEmitter>() }
        bean { mockk<RxPermissions>() }
        bean("io") { TestScheduler() as Scheduler }
        bean("ui") { TestScheduler() as Scheduler }
        bean { mockk<FavoriteStopsRepository>(relaxUnitFun = true) }
        bean { applicationContext }
    }

    override fun onCreate() {
    }
}