package com.aff2.car.core;

import android.util.Pair;

import java.util.List;

public class Utils {

    public static String addUrlParam(String url, List<Pair<String, String>> keyValue) {
        for (Pair<String, String> pair: keyValue) {
            boolean hasParams = url.contains("?");
            if(!hasParams && !url.endsWith("/")) url += "/";

            if(hasParams) url = url + "&" + pair.first + "=" + pair.second;
            else url = url + "?" + pair.first + "=" + pair.second;
        }
        return url;
    }
}
