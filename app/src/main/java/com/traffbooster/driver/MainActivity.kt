package com.traffbooster.driver

import android.content.Intent
import android.widget.FrameLayout
import com.traffbooster.car.core.StartActivity

class MainActivity : StartActivity() {

    override fun onShowAppUi() {
        startActivity(Intent(this, ExampleActivity::class.java))
    }

    override fun initLoadingView(loadingView: FrameLayout?) {

    }
}