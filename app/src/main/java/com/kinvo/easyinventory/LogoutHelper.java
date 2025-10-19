package com.kinvo.easyinventory;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.webkit.CookieManager;
import android.webkit.WebStorage;

public final class LogoutHelper {
    private LogoutHelper() {}

    public static void logoutAndRestart(Activity activity) {
        // 1) Clear secure prefs
        SecurePrefs prefs = SecurePrefs.get(activity.getApplicationContext());
        try {
            // Prefer a semantic logout (resets keys + flags) then hard clear
            prefs.logout();
            prefs.clearAll();
        } catch (Throwable t) {
            Logx.w("LogoutHelper", "Prefs clear failed but continuing logout: " + t);
        }

        // 2) Clear WebView state (cookies + local storage)
        try {
            CookieManager cm = CookieManager.getInstance();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                cm.removeAllCookies(null);   // async is fine; we don't have to wait
            } else {
                cm.removeAllCookie();
            }
            cm.flush();
            WebStorage.getInstance().deleteAllData();
        } catch (Throwable t) {
            Logx.w("LogoutHelper", "Cookie/WebStorage clear failed: " + t);
        }

        // 3) Return to Splash (wipe back stack)
        Intent i = new Intent(activity, SplashActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(i);
        activity.finish();
    }
}

