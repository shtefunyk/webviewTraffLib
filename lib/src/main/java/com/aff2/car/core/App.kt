package com.aff2.car.core

import android.app.Application
import android.util.Log
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.google.firebase.FirebaseApp
import com.aff2.car.R

abstract class App : Application() {

    companion object {
        lateinit var instance: App
    }

    abstract fun getIntroItems(): List<IntroItem>
    abstract fun getIntroBgColor() : Int
    abstract fun getAppUiClassName() : Class<*>

    override fun onCreate() {
        super.onCreate()
        setTheme(R.style.AppThemeLib)
        instance = this
        FirebaseApp.initializeApp(applicationContext)
    }
}