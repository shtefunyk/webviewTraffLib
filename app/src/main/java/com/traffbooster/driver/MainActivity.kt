package com.traffbooster.driver

import com.traffbooster.car.core.StartActivity

class MainActivity : StartActivity() {

    override fun getLoadingViewLayoutRes() = R.layout.loadingg;

    override fun getAppPackageName(): String = "com.traffbooster.driver"
}