package com.kinvo.easyinventory;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

public class WebViewActivity extends AppCompatActivity {
    private static final String TAG = "WebViewActivity";
    public static final String EXTRA_URL = "EXTRA_URL";
    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        webView = findViewById(R.id.webView);

        // 1. Configure WebViewClient for proper navigation handling
        webView.setWebViewClient(new CustomWebViewClient());

        // 2. Enable JavaScript and other performance settings
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // 3. Load the URL
        String url = getIntent().getStringExtra(EXTRA_URL);
        if (url != null && !url.isEmpty()) {
            webView.loadUrl(url);
        } else {
            Log.e(TAG, "URL is null or empty");
            // Handle the error appropriately, e.g., show an error message or finish the activity.
            finish();
        }
    }

    private static class CustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }

    @Override
    protected void onDestroy() {
        // 4. Destroy webview properly
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}