package com.kinvo.easyinventory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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
    private static final String MEMBERSTACK_CALLBACK_PREFIX = BuildConfig.MEMBERSTACK_CALLBACK_PREFIX;

    private String loginUrl;
    private String basicUrl;
    private String premiumUrl;

    // Guard to avoid double-navigation (shouldOverride + JS)
    private final AtomicBoolean handledSuccessOnce = new AtomicBoolean(false);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        webView = findViewById(R.id.webView);

        String authBase = BuildConfig.AUTH_BASE_URL;
        loginUrl = buildUrl(authBase, "login");
        basicUrl = buildUrl(authBase, "basic", "account");
        premiumUrl = buildUrl(authBase, "premium", "account");

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

        Log.d(TAG, "Loading LOGIN_URL: " + loginUrl);
        webView.loadUrl(loginUrl);
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

        boolean isMemberstackCallback = MEMBERSTACK_CALLBACK_PREFIX != null
                && !MEMBERSTACK_CALLBACK_PREFIX.isEmpty()
                && url.contains(MEMBERSTACK_CALLBACK_PREFIX);
        boolean isBasic = basicUrl != null && !basicUrl.isEmpty() && url.startsWith(basicUrl);
        boolean isPremium = premiumUrl != null && !premiumUrl.isEmpty() && url.startsWith(premiumUrl);

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
        // Mirror success flag for MembershipLoginActivity auto-forward
        getSharedPreferences("UserPrefs", MODE_PRIVATE)
                .edit().putBoolean("membershipOk", true).apply();

        // Keep a simple mirror in regular SharedPreferences
        SharedPreferences sp = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        sp.edit().putString("membershipType", membershipType).apply();

        // And persist tier/plan in SecurePrefs used throughout the app
        try {
            SecurePrefs prefs = SecurePrefs.get(this);
            prefs.setLoggedIn(true);

            Tier tier = Tier.fromString(membershipType);
            if (!"Premium".equalsIgnoreCase(membershipType)
                    && !"Basic".equalsIgnoreCase(membershipType)) {
                Tier stored = TierUtils.storedTier(prefs);
                if (stored != null) {
                    tier = stored;
                }
            }

            prefs.setTierName(tier.name());

            String planLabel = membershipType;
            if (planLabel == null || planLabel.trim().isEmpty()
                    || "Authenticated".equalsIgnoreCase(planLabel)) {
                planLabel = TierUtils.displayName(tier);
            }
            prefs.setPlanName(planLabel);
        } catch (Exception e) {
            Log.w(TAG, "SecurePrefs not available yet; continuing without setTier()", e);
        }
    }

    private static String buildUrl(String base, String... segments) {
        if (base == null || base.trim().isEmpty()) {
            return "";
        }
        Uri.Builder builder = Uri.parse(base).buildUpon();
        for (String segment : segments) {
            if (segment != null && !segment.trim().isEmpty()) {
                builder.appendPath(segment);
            }
        }
        return builder.build().toString();
    }

    private void proceedToLoginActivity() {
        Log.d(TAG, "Proceeding to LoginActivity");
        Intent intent = new Intent(WebViewLoginActivity.this, ProviderPickerActivity.class);
        startActivity(intent);
        finish();
        // If you want to go straight to LoginActivity after provider is chosen,
        // WebViewLoginActivity.onResume() already routes accordingly.
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

    @Override
    protected void onResume() {
        super.onResume();
        try {
            SecurePrefs prefs = SecurePrefs.get(this);
            if (prefs.isProviderChosen()) {
                // Provider selected; continue to credential screen
                Intent intent = new Intent(WebViewLoginActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        } catch (Exception ignored) {}
    }
}
