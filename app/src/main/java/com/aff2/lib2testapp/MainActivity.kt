package com.aff2.lib2testapp

import com.aff2.car.core.StartActivity

class MainActivity : StartActivity() {

    override fun getLoadingViewLayoutRes() = R.layout.loadingg;

    override fun getAppPackageName(): String = "com.traffbooster.driver"
}