package com.netless.pptdownload;

import android.util.Log;

import com.netless.pptdownload.library.BuildConfig;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @author fenglibin
 */
class DownloadLogger {
    private DownloadLogger() {
    }

    private static final String TAG = "PptDownload";

    static final boolean DEBUG = BuildConfig.DEBUG;

    public static void d(@NonNull String message) {
        if (DEBUG) {
            Log.d(TAG, message);
        }
    }

    public static void i(@NonNull String message) {
        Log.i(TAG, message);
    }

    public static void e(@NonNull String message) {
        Log.e(TAG, message);
    }

    public static void e(@NonNull String message, @Nullable Throwable throwable) {
        Log.e(TAG, message, throwable);
    }
}
