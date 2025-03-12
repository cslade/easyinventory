package com.kinvo.easyinventory;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

public class WebViewActivity extends AppCompatActivity {

    private static final String TAG = "WebViewActivity";
    public static final String EXTRA_URL = "extra_url";
    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        webView = findViewById(R.id.webView);

        // 1. Configure WebViewClient for proper navigation handling
        // Use an anonymous class instead of CustomWebViewClient.
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Handle navigation within the WebView here if needed.
                return false;
            }
        });

        // 2. Enable JavaScript and other performance settings
        configureWebSettings(webView.getSettings());

        // 3. Load the URL
        loadUrlFromIntent();
    }

    /**
     * Configures the WebSettings for the WebView.
     *
     * @param webSettings The WebSettings object to configure.
     */
    private void configureWebSettings(WebSettings webSettings) {
        webSettings.setJavaScriptEnabled(true); // Enable JavaScript
        webSettings.setDomStorageEnabled(true); // Enable DOM storage

        // Enable images loading
        webSettings.setLoadsImagesAutomatically(true);

        // Set cache mode for better performance
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // Enable database access
        webSettings.setDatabaseEnabled(true);

        // Enable zooming
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        // Enable support for viewport meta tag
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);

        //Improve rendering performance
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);

        // Enable hardware acceleration for better performance.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true); //Enable content debugging
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            webSettings.setAllowUniversalAccessFromFileURLs(true); // Enable access from file URLs
        }
    }

    /**
     * Loads the URL passed in the intent's extra.
     */
    private void loadUrlFromIntent() {
        String url = getIntent().getStringExtra(EXTRA_URL);
        if (url != null && !url.isEmpty()) {
            webView.loadUrl(url);
        } else {
            Log.e(TAG, "URL is null or empty");
            // Handle the error appropriately, e.g., show an error message or finish the activity.
            finish();
        }
    }
}