package io.pivotal.safebus

import android.app.Application
import com.google.android.gms.location.LocationServices
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import io.pivotal.safebus.api.SafeBusApi
import org.koin.android.ext.android.startKoin
import org.koin.dsl.module.Module
import org.koin.dsl.module.applicationContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

open class SafeBusApplication : Application() {

    private val modules: Module = applicationContext {
        bean {
            Retrofit.Builder()
                    .baseUrl("https://safebus.cfapps.io")
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build()
                    .create(SafeBusApi::class.java)
        }
        bean { LocationServices.getFusedLocationProviderClient(applicationContext) }
        bean { params -> MapEmitter(params["activity"]) }
    }

    override fun onCreate() {
        super.onCreate()
        startKoin(this, listOf(modules))
    }
}