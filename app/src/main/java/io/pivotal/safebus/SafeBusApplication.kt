package io.pivotal.safebus

import android.app.Application
import android.support.v4.app.FragmentActivity
import com.google.android.gms.location.LocationServices
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import com.tbruyelle.rxpermissions2.RxPermissions
import io.pivotal.safebus.api.SafeBusApi
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
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
        bean { BitmapCreator(applicationContext) }
        bean { BusIconResource(get()) }
        bean { FavoriteStopsRepository(applicationContext) }
        bean { params -> MapEmitter(params["activity"], get(), get()) }
        bean { params -> RxPermissions(params.get<FragmentActivity>("activity")) }
        factory("ui") { AndroidSchedulers.mainThread() }
        factory("io") { Schedulers.io() }
    }

    override fun onCreate() {
        super.onCreate()
        startKoin(this, listOf(modules))
    }
}