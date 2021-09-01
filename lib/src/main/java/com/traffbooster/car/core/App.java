package com.traffbooster.car.core;

import android.app.Application;

import com.appsflyer.AppsFlyerLib;
import com.google.firebase.FirebaseApp;
import com.kochava.base.Tracker;
import com.onesignal.OneSignal;
import com.traffbooster.car.R;

public abstract class App extends Application {

    protected abstract String getKochavaAppGuid();
    protected abstract String getOneSignalId();
    protected abstract String getAppsflyerId();

    @Override
    public void onCreate() {
        super.onCreate();
        setTheme(R.style.AppThemeLib);
        FirebaseApp.initializeApp(getApplicationContext());

        if(getKochavaAppGuid() != null) initKochava();
        if(getOneSignalId() != null) initOneSignal();
        if(getAppsflyerId() != null) initAppsFlyer();
    }

    private void initKochava() {
        Tracker.configure(new Tracker.Configuration(getApplicationContext())
                .setAppGuid(getKochavaAppGuid()));
    }

    private void initOneSignal() {
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);
        OneSignal.initWithContext(this);
        OneSignal.setAppId(getOneSignalId());
    }

    private void initAppsFlyer() {
        AppsFlyerLib.getInstance().init(getAppsflyerId(), null, this);
        AppsFlyerLib.getInstance().start(this);
    }
}
