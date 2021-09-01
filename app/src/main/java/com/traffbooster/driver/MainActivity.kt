package com.traffbooster.driver

import android.content.Intent
import com.traffbooster.car.core.StartActivity

class MainActivity : StartActivity() {

    override fun onShowAppUi() {
        startActivity(Intent(this, ExampleActivity::class.java))
    }
}