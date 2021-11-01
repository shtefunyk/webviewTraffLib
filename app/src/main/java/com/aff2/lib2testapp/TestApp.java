package com.aff2.lib2testapp;

import androidx.annotation.NonNull;

import com.aff2.car.core.App;
import com.aff2.car.core.IntroItem;

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

    @NonNull
    @Override
    public String getAppsflyerId() {
        return "tuRVfbsRT7QoUMoFqjUL8a";
    }
}
