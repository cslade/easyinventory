package com.kinvo.easyinventory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class WebViewLoginActivity extends AppCompatActivity {

    private WebView webView;
    private static final String BASIC_URL = "https://www.easyinventory.io/basic/account";
    private static final String PREMIUM_URL = "https://www.easyinventory.io/premium/account";
    private static final String LOGIN_URL = "https://www.easyinventory.io/login";
    private static final String MEMBERSTACK_CALLBACK = "https://client.memberstack.com/auth/callback?code=";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);


        webView = findViewById(R.id.webView);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);

        // Enable third-party cookies for Facebook and Google login
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        // Adding JavaScript Interface for Communication
        webView.addJavascriptInterface(new WebAppInterface(), "AndroidInterface");

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                if (url.contains(MEMBERSTACK_CALLBACK)) {
                    saveMembershipType("Authenticated");
                    proceedToLoginActivity();
                    return true;
                }

                if (url.contains(BASIC_URL)) {
                    saveMembershipType("Basic");
                    proceedToLoginActivity();
                    return true;
                }

                if (url.contains(PREMIUM_URL)) {
                    saveMembershipType("Premium");
                    proceedToLoginActivity();
                    return true;
                }

                return false;
            }


            @Override
            public void onPageFinished(WebView view, String url) {
                view.loadUrl("javascript:(function() { " +
                        "if(window.location.href.includes('auth/callback?code=')) { " +
                        "    AndroidInterface.onLoginSuccess(window.location.href); " +
                        "} " +
                        "})();");
            }
        });

        webView.loadUrl(LOGIN_URL);
    }

    private void saveMembershipType(String membershipType) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("membershipType", membershipType);
        editor.apply();
    }

    private void proceedToLoginActivity() {
        Intent intent = new Intent(WebViewLoginActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void onLoginSuccess(String url) {
            if (url.contains("auth/callback?code=")) {
                saveMembershipType("Authenticated");
            }
            proceedToLoginActivity();
        }
    }
}