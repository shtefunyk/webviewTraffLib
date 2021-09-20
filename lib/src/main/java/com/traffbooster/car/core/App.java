package com.traffbooster.car.core;

import android.app.Application;

import com.appsflyer.AppsFlyerLib;
import com.google.firebase.FirebaseApp;
import com.kochava.base.Tracker;
import com.onesignal.OneSignal;
import com.traffbooster.car.R;

public abstract class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        setTheme(R.style.AppThemeLib);
        FirebaseApp.initializeApp(getApplicationContext());
    }
}
