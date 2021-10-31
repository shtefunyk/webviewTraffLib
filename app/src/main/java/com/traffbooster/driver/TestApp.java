package com.traffbooster.driver;

import android.content.Intent;

import androidx.annotation.NonNull;

import com.traffbooster.car.core.App;
import com.traffbooster.car.core.IntroItem;

import java.util.ArrayList;
import java.util.List;

public class TestApp extends App {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @NonNull
    @Override
    public List<IntroItem> getIntroItems() {
        List<IntroItem> list = new ArrayList<>();
        list.add(new IntroItem("Title", "Description", R.drawable.affiliate));
        list.add(new IntroItem("Title", "Description", R.drawable.affiliate));
        list.add(new IntroItem("Title", "Description", R.drawable.affiliate));
        return list;
    }

    @NonNull
    @Override
    public Class<?> getAppUiClassName() {
        return ExampleActivity.class;
    }

    @Override
    public int getIntroBgColor() {
        return R.color.colorIntro;
    }
}
