package com.traffbooster.car.core;

import android.app.Application;

import com.facebook.FacebookSdk;
import com.google.firebase.FirebaseApp;
import com.kochava.base.Tracker;
import com.traffbooster.car.R;

import static com.facebook.FacebookSdk.setAutoInitEnabled;
import static com.facebook.FacebookSdk.setAutoLogAppEventsEnabled;

public abstract class App extends Application {

    protected abstract String getKochavaAppGuid();

    @Override
    public void onCreate() {
        super.onCreate();
        setTheme(R.style.AppTheme);

        FirebaseApp.initializeApp(getApplicationContext());

        Tracker.configure(new Tracker.Configuration(getApplicationContext())
                .setAppGuid(getKochavaAppGuid()));


    }
}
