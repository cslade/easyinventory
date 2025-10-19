package com.kinvo.easyinventory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.atomic.AtomicBoolean;

public class WebViewLoginActivity extends AppCompatActivity {

    private static final String TAG = "WebViewLoginActivity";

    private WebView webView;

    // Memberstack OAuth callback (prod)
    private static final String MEMBERSTACK_CALLBACK = "https://client.memberstack.com/auth/callback?code=";

    // Build-variant aware URLs
    private static final boolean IS_DEMO = BuildConfig.FLAVOR != null && BuildConfig.FLAVOR.equals("demo");

    private static final String LOGIN_URL =
            IS_DEMO ? "https://easyinventory.webflow.io/login"
                    : "https://www.easyinventory.io/login";

    private static final String BASIC_URL =
            IS_DEMO ? "https://easyinventory.webflow.io/basic/account"
                    : "https://www.easyinventory.io/basic/account";

    private static final String PREMIUM_URL =
            IS_DEMO ? "https://easyinventory.webflow.io/premium/account"
                    : "https://www.easyinventory.io/premium/account";

    // Guard to avoid double-navigation (shouldOverride + JS)
    private final AtomicBoolean handledSuccessOnce = new AtomicBoolean(false);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        webView = findViewById(R.id.webView);

        // WebView settings
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setJavaScriptCanOpenWindowsAutomatically(true);

        // Enable 3P cookies for Google/Facebook SSO flows inside WebView
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        // JS bridge for a resilient fallback (when SPA redirects happen via history API)
        webView.addJavascriptInterface(new WebAppInterface(), "AndroidInterface");

        webView.setWebViewClient(new WebViewClient() {

            @Override // API < 21
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(TAG, "shouldOverrideUrlLoading(<21): " + url);
                return maybeHandleAuthSuccess(url);
            }

            @Override // API 21+
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = (request != null && request.getUrl() != null) ? request.getUrl().toString() : "";
                Log.d(TAG, "shouldOverrideUrlLoading(21+): " + url);
                return maybeHandleAuthSuccess(url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "onPageFinished: " + url);

                if (maybeHandleAuthSuccess(url)) return;

                // JS fallback: covers SPA-style redirects
                view.loadUrl(
                        "javascript:(function(){"
                                + "try {"
                                + "  var href = window.location.href || '';"
                                + "  if (href.indexOf('auth/callback?code=') !== -1"
                                + "      || href.indexOf('/basic/account') !== -1"
                                + "      || href.indexOf('/premium/account') !== -1) {"
                                + "    if (window.AndroidInterface && AndroidInterface.onLoginSuccess) {"
                                + "      AndroidInterface.onLoginSuccess(href);"
                                + "    }"
                                + "  }"
                                + "} catch(e) { /* no-op */ }"
                                + "})();"
                );
            }
        });

        Log.d(TAG, "Loading LOGIN_URL: " + LOGIN_URL);
        webView.loadUrl(LOGIN_URL);
    }

    /**
     * Decide whether a URL means auth success; if so, short-circuit WebView and return to the app.
     */
    private boolean maybeHandleAuthSuccess(String url) {
        if (handledSuccessOnce.get()) {
            Log.d(TAG, "maybeHandleAuthSuccess: already handled, ignoring.");
            return true;
        }
        if (url == null) return false;

        boolean isMemberstackCallback = url.contains(MEMBERSTACK_CALLBACK);
        boolean isBasic = url.startsWith(BASIC_URL);
        boolean isPremium = url.startsWith(PREMIUM_URL);

        if (isMemberstackCallback || isBasic || isPremium) {
            String membershipType = isPremium ? "Premium" : (isBasic ? "Basic" : "Authenticated");
            Log.d(TAG, "Auth success detected (" + membershipType + ") from url: " + url);

            saveMembershipTypeAndTier(membershipType);
            proceedToLoginActivity();

            handledSuccessOnce.set(true);
            return true;
        }
        return false;
    }

    private void saveMembershipTypeAndTier(String membershipType) {
        // Keep a simple mirror in regular SharedPreferences
        SharedPreferences sp = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        sp.edit().putString("membershipType", membershipType).apply();

        // And persist tier/plan in SecurePrefs used throughout the app
        try {
            SecurePrefs prefs = SecurePrefs.get(this);
            prefs.setLoggedIn(true);

            // Choose tier: premium/basic from URL; otherwise default from flavor.
            Tier tier =
                    "Premium".equalsIgnoreCase(membershipType) ? Tier.PREMIUM :
                            "Basic".equalsIgnoreCase(membershipType)   ? Tier.BASIC   :
                                    TierUtils.fromFlavor(BuildConfig.FLAVOR);

            prefs.setTier(tier);
            prefs.setPlanName(membershipType);
        } catch (Exception e) {
            Log.w(TAG, "SecurePrefs not available yet; continuing without setTier()", e);
        }
    }

    private void proceedToLoginActivity() {
        Log.d(TAG, "Proceeding to LoginActivity");
        Intent intent = new Intent(WebViewLoginActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    // JS bridge used by onPageFinished() injection
    public class WebAppInterface {
        @JavascriptInterface
        public void onLoginSuccess(String url) {
            Log.d(TAG, "JS onLoginSuccess: " + url);
            if (!handledSuccessOnce.get()) {
                String type = (url.contains("/premium/account")) ? "Premium"
                        : (url.contains("/basic/account"))   ? "Basic"
                        : "Authenticated";
                saveMembershipTypeAndTier(type);
                proceedToLoginActivity();
                handledSuccessOnce.set(true);
            }
        }
    }
}
