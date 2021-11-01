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

    private var listener: IResultListener? = null
    private var afData: String? = null
    private var failed: Boolean? = null

    fun getAfData(listener: IResultListener) {
        this.listener = listener
        if(afData != null) listener.success(afData)
        else if(failed != null) listener.failed()
    }

    abstract fun getIntroItems(): List<IntroItem>
    abstract fun getIntroBgColor() : Int
    abstract fun getAppUiClassName() : Class<*>
    abstract fun getAppsflyerId() : String

    override fun onCreate() {
        super.onCreate()
        setTheme(R.style.AppThemeLib)
        instance = this
        FirebaseApp.initializeApp(applicationContext)
        initAppsflyer(getAppsflyerId())
    }

    private fun initAppsflyer(id: String) {
        val conversionListener: AppsFlyerConversionListener = object : AppsFlyerConversionListener {
            override fun onConversionDataSuccess(conversionData: Map<String, Any>) {
                val params = StringBuilder("&")
                for (attrName in conversionData.keys) {
                    params.append(attrName).append("=").append(conversionData[attrName]).append("&")
                }
                val result = params.toString().replace(" ", "_")
                afData = result
                if(listener != null) {
                    listener!!.success(result)
                    listener = null
                }
            }

            override fun onConversionDataFail(errorMessage: String) {
                failed = true
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

        AppsFlyerLib.getInstance().init(id, conversionListener, this)
        AppsFlyerLib.getInstance().start(this)

    }
}