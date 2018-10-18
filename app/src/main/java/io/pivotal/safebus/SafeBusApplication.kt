package io.pivotal.safebus

import android.app.Application
import android.support.v4.app.FragmentActivity
import com.google.android.gms.location.LocationServices
import com.tbruyelle.rxpermissions2.RxPermissions
import io.pivotal.safebus.api.SafeBusApi
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.startKoin
import org.koin.dsl.module.Module
import org.koin.dsl.module.module
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

open class SafeBusApplication : Application() {

    private val modules: Module = module {
        single {
            Retrofit.Builder()
                    .baseUrl("https://safebus.cfapps.io")
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build()
                    .create(SafeBusApi::class.java)
        }
        single { LocationServices.getFusedLocationProviderClient(applicationContext) }
        single { BitmapCreator(applicationContext) }
        single { BusIconResource(get()) }
        single { FavoriteStopsRepository(applicationContext) }
        single { (activity: FragmentActivity) -> MapEmitter(activity, get(), get()) }
        single { (activity: FragmentActivity) -> RxPermissions(activity) }
        factory("ui") { AndroidSchedulers.mainThread() }
        factory("io") { Schedulers.io() }
    }

    override fun onCreate() {
        super.onCreate()
        startKoin(this, listOf(modules))
    }
}