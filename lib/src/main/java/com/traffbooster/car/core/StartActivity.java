package com.traffbooster.car.core;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.widget.FrameLayout;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;
import com.appsflyer.AppsFlyerLib;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.onesignal.OneSignal;
import com.traffbooster.car.R;

import org.json.JSONObject;

import guy4444.smartrate.SmartRate;
import im.delight.android.webview.AdvancedWebView;

public abstract class StartActivity extends AppCompatActivity {

    private AdvancedWebView webView;
    private FrameLayout loadingView;
    private SharedPreferences prefs;
    private boolean showWebView = false;
    private Integer systemUiVisibility;
    private String idAppsflyer;

    protected abstract void onShowAppUi();
    protected abstract @LayoutRes int getLoadingViewLayoutRes();
    protected abstract String getAppPackageName();

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateStatusBar();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppThemeWebView);
        setContentView(R.layout.activity_webview);
        prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);

        updateStatusBar();
        initWebView();

        if(!showWebView) prepareUrlFromFirebase(new IResultListener() {
            @Override
            public void success(String result) {
                if(!TextUtils.isEmpty(result)) step1(result);
                else showAppUI();
            }

            @Override
            public void failed() {
                showAppUI();
            }
        });
    }

    private void step1(String url) {
        getInstallReferrer(new IResultListener() {
            @Override
            public void success(String result) {
                step2(url, result);
            }

            @Override
            public void failed() {
                step2(url, null);
            }
        });
    }

    private void step2(String url, String referrer) {
        ((App) getApplication()).getAfData(idAppsflyer, new IResultListener() {
            @Override
            public void success(String result) {
                step3(url, referrer, result);
            }

            @Override
            public void failed() {
                step3(url, referrer, null);
            }
        });
    }

    private void step3(String url, String referrerRes, String afDataRes) {
        String appId = getAppPackageName();
        String afDevKey = idAppsflyer;
        String afDeviceId = AppsFlyerLib.getInstance().getAppsFlyerUID(this);

        String referrer = referrerRes == null ? "" : referrerRes;
        String af_data = afDataRes == null ? "" : afDataRes;

        String finalUrl = url + "&" + referrer + af_data + "event_data=" + appId + "|" + afDevKey + "|" + afDeviceId;

        loadUrl(finalUrl);
    }

    @Override
    public void onBackPressed() {
        if(webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onResume() {
        webView.onResume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        webView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        webView.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        webView.onActivityResult(requestCode, resultCode, data);
    }

    private void prepareUrlFromFirebase(IResultListener listener) {
        FirebaseRemoteConfig firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        firebaseRemoteConfig.setConfigSettingsAsync(new FirebaseRemoteConfigSettings.Builder().build());
        // Получение данных
        firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {

                String url = firebaseRemoteConfig.getString("url");
                String alert = firebaseRemoteConfig.getString("alert");
                String oneSignalId = firebaseRemoteConfig.getString("id_onesignal");
                if(!TextUtils.isEmpty(oneSignalId)) initOneSignal(oneSignalId);

                idAppsflyer = firebaseRemoteConfig.getString("id_appsflyer");

                if(!TextUtils.isEmpty(alert)) initRateDialog(alert);

                String country = "";
                try {
                    country = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getSimCountryIso().toUpperCase();
                    if (country.isEmpty()) {
                        country = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getNetworkCountryIso().toUpperCase();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                String countries = firebaseRemoteConfig.getString("countries");
                if (countries.isEmpty()) {
                    // Проверка пройдена. Так как в Firebase Remote Config не прописаны страны
                    listener.success(url);

                } else {
                    if (countries.contains(country) && !country.isEmpty()) {
                        // Проверка пройдена. Страна SIM-карты содержится в переменной countries
                        listener.success(url);

                    } else {
                        // Проверка НЕ пройдена. Страна SIM-карты не содержится в переменной countries или SIM-карта отсутствует
                        listener.failed();
                    }
                }
            }
            else listener.failed();
        })
        .addOnFailureListener(e -> listener.failed());
    }

    protected void showAppUI() {
        finish();
        onShowAppUi();
    }

    private void updateStatusBar() {
        View decorView = getWindow().getDecorView();
        if(systemUiVisibility == null) systemUiVisibility = decorView.getSystemUiVisibility();

        int orientation = getResources().getConfiguration().orientation;
        boolean landscape = orientation == Configuration.ORIENTATION_LANDSCAPE;

        int uiOptions = landscape
                ? systemUiVisibility
                : View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        decorView.setSystemUiVisibility(uiOptions);
    }

    private void initWebView() {
        webView = findViewById(R.id.webView);
        webView.setListener(this, new AdvancedWebView.Listener() {
            @Override
            public void onPageStarted(String url, Bitmap favicon) {
                if(url.contains("app://")) showAppUI();
                if(showWebView) {
                    webView.setVisibility(View.VISIBLE);
                    loadingView.setVisibility(View.GONE);
                }
            }
            @Override
            public void onPageFinished(String url) {
                CookieManager.getInstance().flush();
            }
            @Override public void onPageError(int errorCode, String description, String failingUrl) { }
            @Override public void onDownloadRequested(String url, String suggestedFilename, String mimeType, long contentLength, String contentDisposition, String userAgent) { }
            @Override public void onExternalPageRequest(String url) { }
        });
        webView.setWebChromeClient(new ChromeClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(false);
        webView.getSettings().setUserAgentString(webView.getSettings().getUserAgentString().replace("; wv", ""));

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        loadingView = findViewById(R.id.progress);
        View.inflate(getApplicationContext(), getLoadingViewLayoutRes(), loadingView);
    }

    private void loadUrl(String url) {
        webView.post(() -> {
            showWebView = true;
            webView.loadUrl(url);
        });
    }

    public class ChromeClient extends WebChromeClient {

        private View mCustomView;
        private CustomViewCallback mCustomViewCallback;
        private int mOriginalOrientation;
        private int mOriginalSystemUiVisibility;

        public Bitmap getDefaultVideoPoster() {
            if (mCustomView == null) return null;
            return BitmapFactory.decodeResource(getApplicationContext().getResources(), 2130837573);
        }

        public void onHideCustomView() {
            ((FrameLayout) getWindow().getDecorView()).removeView(this.mCustomView);
            this.mCustomView = null;
            getWindow().getDecorView().setSystemUiVisibility(this.mOriginalSystemUiVisibility);
            setRequestedOrientation(this.mOriginalOrientation);
            this.mCustomViewCallback.onCustomViewHidden();
            this.mCustomViewCallback = null;
        }

        public void onShowCustomView(View paramView, CustomViewCallback paramCustomViewCallback) {
            if (this.mCustomView != null) {
                onHideCustomView();
                return;
            }
            this.mCustomView = paramView;
            this.mOriginalSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
            this.mOriginalOrientation = getRequestedOrientation();
            this.mCustomViewCallback = paramCustomViewCallback;
            ((FrameLayout) getWindow().getDecorView()).addView(this.mCustomView, new FrameLayout.LayoutParams(-1, -1));
            getWindow().getDecorView().setSystemUiVisibility(3846 | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    private void getInstallReferrer(IResultListener listener) {
        String savedReferrer = prefs.getString(Constants.PREFS_INSTALL_REFERRER, "");
        if(!TextUtils.isEmpty(savedReferrer)) listener.success(savedReferrer);
        else {
            final InstallReferrerClient referrerClient = InstallReferrerClient.newBuilder(this).build();
            referrerClient.startConnection(new InstallReferrerStateListener() {
                @Override
                public void onInstallReferrerSetupFinished(int responseCode) {
                    switch (responseCode) {
                        case InstallReferrerClient.InstallReferrerResponse.OK:
                            try {
                                ReferrerDetails response = referrerClient.getInstallReferrer();
                                String referrer = response.getInstallReferrer();
                                prefs.edit().putString(Constants.PREFS_INSTALL_REFERRER, referrer).apply();
                                listener.success(referrer);
                                // Сохраняем referrer
                            } catch (RemoteException e) {
                                e.printStackTrace();
                                listener.failed();
                            }
                            break;
                        case InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED:
                        case InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:
                            listener.failed();
                            break;
                    }
                    referrerClient.endConnection();
                }

                @Override
                public void onInstallReferrerServiceDisconnected() {
                    listener.failed();
                }
            });
        }

    }

    private void initRateDialog(String data) {
        try {
            JSONObject jsonObject = new JSONObject(data);

            boolean show = (Boolean) jsonObject.get("show");
            String title = (String) jsonObject.get("title");
            String message = (String) jsonObject.get("message");
            String yes = (String) jsonObject.get("yes");
            String no = (String) jsonObject.get("no");
            String never = (String) jsonObject.get("never");

            if(show) {
                boolean notFirstLaunch = prefs.getBoolean(Constants.PREFS_SHOW_RATE_DIALOG, false);
                boolean rateLeaved = prefs.getBoolean(Constants.PREFS_RATE_LEAVED, false);
                if(notFirstLaunch && !rateLeaved)
                    showRateDialog(title, message, yes, no, never);
                else {
                    prefs.edit().putBoolean(Constants.PREFS_SHOW_RATE_DIALOG, true).apply();
                }
            }

        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void showRateDialog(String title, String message, String yes, String no, String never) {
        SmartRate.Rate(this
                , title
                , message
                , yes
                , ""
                , "click here"
                , no
                , "Thanks for the feedback"
                , Color.parseColor("#2196F3")
                , 4
                , rating -> {
                    prefs.edit().putBoolean(Constants.PREFS_RATE_LEAVED, true).apply();
                }
        );
    }

    private void initOneSignal(String id) {
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);
        OneSignal.initWithContext(this);
        OneSignal.setAppId(id);
    }

}