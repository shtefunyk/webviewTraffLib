package com.traffbooster.car.core

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment
import com.traffbooster.car.R

open class Intro: AppIntro() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make sure you don't call setContentView!

        (application as App).getIntroItems().forEach {
            addSlide(
                AppIntroFragment.newInstance(
                    title = it.title,
                    description = it.description,
                    imageDrawable = it.icon,
                    backgroundColor = resources.getColor((application as App).getIntroBgColor()),
                    titleColor = resources.getColor(R.color.black),
                    descriptionColor = resources.getColor(R.color.black)
                ))
        }

    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        startActivity(Intent(this, (application as App).getAppUiClassName()))
        finish()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        startActivity(Intent(this, (application as App).getAppUiClassName()))
        finish()
    }
}