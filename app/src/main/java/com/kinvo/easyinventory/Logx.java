package com.kinvo.easyinventory;

import android.util.Log;

public final class Logx {
    private Logx() {}

    public static void d(String tag, String msg) {
        if (BuildConfig.DEBUG) Log.d(tag, msg);
    }

    public static void i(String tag, String msg) {
        if (BuildConfig.DEBUG) Log.i(tag, msg);
    }

    public static void v(String tag, String msg) {
        if (BuildConfig.DEBUG) Log.v(tag, msg);
    }

    // Always keep warnings and errors
    public static void w(String tag, String msg) { Log.w(tag, msg); }
    public static void e(String tag, String msg) { Log.e(tag, msg); }
    public static void e(String tag, String msg, Throwable tr) { Log.e(tag, msg, tr); }
}
