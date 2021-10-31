package com.traffbooster.car.core

import android.app.Application
import android.util.Log
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.google.firebase.FirebaseApp
import com.traffbooster.car.R

abstract class App : Application() {

    companion object {
        lateinit var instance: App
    }

    private var listener: IResultListener? = null

    fun getAfData(id: String, listener: IResultListener) {
        this.listener = listener
        initAppsflyer(id)
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

    private fun initAppsflyer(id: String) {
        val conversionListener: AppsFlyerConversionListener = object : AppsFlyerConversionListener {
            override fun onConversionDataSuccess(conversionData: Map<String, Any>) {
                val params = StringBuilder("&")
                for (attrName in conversionData.keys) {
                    params.append(attrName).append("=").append(conversionData[attrName]).append("&")
                }
                if(listener != null) {
                    listener!!.success(params.toString().replace(" ", "_"))
                    listener = null
                }
            }

            override fun onConversionDataFail(errorMessage: String) {
                Log.d("LOG_TAG", "error getting conversion data: $errorMessage")
                if (listener != null) {
                    listener!!.failed()
                    listener = null
                }
            }

            override fun onAppOpenAttribution(attributionData: Map<String, String>) {
                for (attrName in attributionData.keys) {
                    Log.d("LOG_TAG", "attribute: " + attrName + " = " + attributionData[attrName])
                }
            }

            override fun onAttributionFailure(errorMessage: String) {
                Log.d("LOG_TAG", "error onAttributionFailure : $errorMessage")
            }
        }

        if(id.isNullOrEmpty()) listener?.failed()
        else {
            AppsFlyerLib.getInstance().init(id, conversionListener, this)
            AppsFlyerLib.getInstance().start(this)
        }

    }
}