package com.netless.pptdownload;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @author fenglibin
 */
class DownloadLogger {
    private DownloadLogger() {
    }

    private static final String TAG = "DownloadLogger";

    static final boolean DEBUG = false;

    public static void d(@NonNull String message) {
        if (DEBUG) {
            Log.d(TAG, message);
        }
    }

    public static void i(@NonNull String message) {
        Log.i(TAG, message);
    }

    public static void e(@NonNull String message, @Nullable Throwable throwable) {
        Log.e(TAG, message, throwable);
    }
}
