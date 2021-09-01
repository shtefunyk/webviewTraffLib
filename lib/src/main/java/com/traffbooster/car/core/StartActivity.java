package com.traffbooster.car.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
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
import android.widget.ProgressBar;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.traffbooster.car.R;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import im.delight.android.webview.AdvancedWebView;

public abstract class StartActivity extends AppCompatActivity implements AdvancedWebView.Listener {

    private AdvancedWebView webView;
    private ProgressBar progressBar;
    private boolean showWebView = false;

    protected abstract void onShowAppUi();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppThemeWebView);
        setContentView(R.layout.activity_webview);
        progressBar = findViewById(R.id.progress);

        initStatusBar();
        initWebView();
        mainInit();
        //printFacebookKeyHash();
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

    @Override
    public void onPageStarted(String url, Bitmap favicon) {
        if(showWebView) {
            webView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPageFinished(String url) {
        CookieManager.getInstance().flush();
    }

    @Override
    public void onPageError(int errorCode, String description, String failingUrl) {}
    @Override
    public void onDownloadRequested(String url, String suggestedFilename, String mimeType, long contentLength, String contentDisposition, String userAgent) { }
    @Override
    public void onExternalPageRequest(String url) {}

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

    private void mainInit() {
        if (isNetworkConnected()) {
            checkStatus(new ISuccessListener() {
                @Override
                public void success(List<Boolean> result) {
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

    protected void showAppUI() {
        finish();
        onShowAppUi();
    }

    private void initStatusBar() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private void initWebView() {
        webView = findViewById(R.id.webView);
        webView.setListener(this, this);
        webView.setWebChromeClient(new ChromeClient());
        webView.getSettings().setDomStorageEnabled(true);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
    }

    private void showAds() {
        downloadUrl(new IResultListener() {
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

    private void checkStatus(ISuccessListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("data").document("app");
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists() && document.getData() != null
                        && document.getData().get("show") != null) {
                    Boolean show = (Boolean) document.getData().get("show");
                    if(show != null && !show) {
                        ArrayList<Boolean> res = new ArrayList<>();
                        res.add(show);
                        listener.success(res);
                    }
                    else listener.failed();
                } else listener.failed();
            } else listener.failed();
        }).addOnFailureListener(e -> listener.failed());
    }

    private void downloadUrl(IResultListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("data").document("app");
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists() && document.getData() != null
                        && document.getData().get("url") != null) {

                    String url = document.getData().get("url").toString();

                    if (!TextUtils.isEmpty(url)) listener.success(url);
                    else listener.failed();

                } else listener.failed();
            } else listener.failed();
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
}