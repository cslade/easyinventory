package com.kinvo.easyinventory;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class WebViewActivity extends AppCompatActivity {

    private static final String TAG = "WebViewActivity";
    public static final String EXTRA_URL = "extra_url";
    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        // ✅ Setup Toolbar with Back Button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Terms of Service");
        }

        // ✅ Initialize WebView
        webView = findViewById(R.id.webView);
        configureWebSettings(webView.getSettings());

        // ✅ Ensure WebView handles redirects properly
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        // ✅ Load the URL
        loadUrlFromIntent();

    }

    /**
     * Configures the WebSettings for the WebView.
     */
    private void configureWebSettings(WebSettings webSettings) {
        webSettings.setJavaScriptEnabled(false);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setDatabaseEnabled(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);

        // Improve performance
        WebView.setWebContentsDebuggingEnabled(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
    }

    /**
     * Loads the URL passed in the intent.
     */
    private void loadUrlFromIntent() {
        String url = getIntent().getStringExtra(EXTRA_URL);
        if (url != null && !url.isEmpty()) {
            webView.loadUrl(url);
        } else {
            Log.e(TAG, "URL is null or empty");
            finish();
        }
    }

    // ✅ Handle Back Button Click
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
